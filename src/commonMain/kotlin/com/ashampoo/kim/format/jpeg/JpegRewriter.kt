/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
 * Copyright 2007-2023 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashampoo.kim.format.jpeg

import com.ashampoo.kim.common.BinaryFileParser
import com.ashampoo.kim.common.ExifOverflowException
import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.ImageWriteException
import com.ashampoo.kim.common.getRemainingBytes
import com.ashampoo.kim.format.jpeg.JpegConstants.JPEG_BYTE_ORDER
import com.ashampoo.kim.format.jpeg.elements.JpegBytesElement
import com.ashampoo.kim.format.jpeg.elements.UnknownSegment
import com.ashampoo.kim.format.jpeg.iptc.IptcBlock
import com.ashampoo.kim.format.jpeg.iptc.IptcConstants
import com.ashampoo.kim.format.jpeg.iptc.IptcMetadata
import com.ashampoo.kim.format.jpeg.iptc.IptcParser
import com.ashampoo.kim.format.jpeg.iptc.IptcWriter
import com.ashampoo.kim.format.jpeg.jfif.JFIFUtils.isAppSegment
import com.ashampoo.kim.format.jpeg.jfif.JFIFUtils.isExifSegment
import com.ashampoo.kim.format.jpeg.jfif.JFIFUtils.isIptcSegment
import com.ashampoo.kim.format.jpeg.jfif.JFIFUtils.isXmpSegment
import com.ashampoo.kim.format.jpeg.jfif.JFIFUtils.write
import com.ashampoo.kim.format.tiff.write.TiffImageWriterBase
import com.ashampoo.kim.format.tiff.write.TiffImageWriterLossless
import com.ashampoo.kim.format.tiff.write.TiffImageWriterLossy
import com.ashampoo.kim.format.tiff.write.TiffOutputSet
import com.ashampoo.kim.input.ByteReader
import com.ashampoo.kim.output.ByteArrayByteWriter
import com.ashampoo.kim.output.ByteWriter
import io.ktor.utils.io.core.toByteArray

/**
 * Interface for Exif write/update/remove functionality for Jpeg/JFIF images.
 */
object JpegRewriter : BinaryFileParser() {

    init {
        byteOrder = JPEG_BYTE_ORDER
    }

    private fun insertAfterLastAppSegments(
        segments: List<JpegBytesElement>,
        newSegments: List<JpegBytesElement>
    ): List<JpegBytesElement> {
        if (segments.isEmpty())
            throw ImageWriteException("JPEG file has no APP segments.")

        val lastAppIndex = segments.indexOfLast { segment ->
            segment is UnknownSegment && segment.isAppSegment()
        }

        return buildList(segments.size + newSegments.size) {
            addAll(segments)
            if (lastAppIndex == -1)
                addAll(1, newSegments)
            else
                addAll(lastAppIndex + 1, newSegments)
        }
    }

    fun updateExifMetadataLossless(byteReader: ByteReader, byteWriter: ByteWriter, outputSet: TiffOutputSet) {

        val oldSegments = JpegUtils.readJFIF(byteReader)

        val (exifSegmentPieces, oldSegmentsWithoutExif) = oldSegments.partition {
            it is UnknownSegment && it.isExifSegment()
        }

        val writer: TiffImageWriterBase

        if (!exifSegmentPieces.isEmpty()) {

            val exifPiece = exifSegmentPieces.first() as UnknownSegment

            val exifBytes = exifPiece.segmentBytes.getRemainingBytes(6)

            writer = TiffImageWriterLossless(outputSet.byteOrder, exifBytes)

        } else
            writer = TiffImageWriterLossy(outputSet.byteOrder)

        val newBytes = writeExifSegment(
            writer = writer,
            outputSet = outputSet,
            includeEXIFPrefix = true
        )

        writeSegmentsReplacingExif(byteWriter, oldSegmentsWithoutExif, newBytes)
    }

    private fun writeSegmentsReplacingExif(
        byteWriter: ByteWriter,
        oldSegments: List<JpegBytesElement>,
        newBytes: ByteArray?
    ) {

        val newSegments = oldSegments.toMutableList()

        byteWriter.write(JpegConstants.SOI)

        if (newBytes != null) {
            if (newBytes.size > JpegConstants.MAX_SEGMENT_SIZE)
                throw ExifOverflowException("APP1 Segment is too long: " + newBytes.size)

            val firstSegment = newSegments.first()
            val exifSegment = UnknownSegment(JpegConstants.JPEG_APP1_MARKER, newBytes)

            if (firstSegment.marker == JpegConstants.JFIF_MARKER) {
                newSegments.add(1, exifSegment)
            } else {
                newSegments.add(0, exifSegment)
            }
        }

        for (piece in newSegments) {
            piece.write(byteWriter, byteOrder)
        }
    }

    private fun writeExifSegment(
        writer: TiffImageWriterBase,
        outputSet: TiffOutputSet,
        includeEXIFPrefix: Boolean
    ): ByteArray {

        val byteWriter = ByteArrayByteWriter()

        if (includeEXIFPrefix) {
            byteWriter.write(JpegConstants.EXIF_IDENTIFIER_CODE)
            byteWriter.write(0)
            byteWriter.write(0)
        }

        writer.write(byteWriter, outputSet)

        return byteWriter.toByteArray()
    }

    /**
     * Reads a Jpeg image, replaces the IPTC data in the App13 segment but
     * leaves the other data in that segment (if present) unchanged and writes
     * the result to a stream.
     */
    fun writeIPTC(byteReader: ByteReader, byteWriter: ByteWriter, newData: IptcMetadata) {

        val oldPieces = JpegUtils.readJFIF(byteReader).toList()

        val (photoshopApp13Segments, oldPiecesWithoutPhotoshopApp13Segments) =
            oldPieces.partition { piece -> piece is UnknownSegment && piece.isIptcSegment() }

        if (photoshopApp13Segments.size > 1)
            throw ImageReadException("Image contains more than one Photoshop App13 segment.")

        val newBlock = IptcBlock(
            blockType = IptcConstants.IMAGE_RESOURCE_BLOCK_IPTC_DATA,
            blockNameBytes = IptcParser.EMPTY_BYTE_ARRAY,
            blockData = IptcWriter.writeIPTCBlock(newData.records)
        )

        val newIptc = IptcMetadata(
            records = newData.records,
            rawBlocks = newData.nonIptcBlocks + newBlock
        )

        val iptcSegment = UnknownSegment(
            marker = JpegConstants.JPEG_APP13_MARKER,
            segmentBytes = IptcWriter.writePhotoshopApp13Segment(newIptc)
        )

        val mergedPieces = insertAfterLastAppSegments(
            oldPiecesWithoutPhotoshopApp13Segments,
            listOf(iptcSegment)
        )

        byteWriter.write(JpegConstants.SOI)

        for (piece in mergedPieces)
            piece.write(byteWriter, byteOrder)
    }

    fun updateXmpXml(byteReader: ByteReader, byteWriter: ByteWriter, xmpXml: String) {

        val allPieces = JpegUtils.readJFIF(byteReader)

        val piecesWithoutXmpSegments =
            allPieces.filterNot { piece -> piece is UnknownSegment && piece.isXmpSegment() }.toList()

        val newPieces = mutableListOf<UnknownSegment>()

        val xmpXmlBytes = xmpXml.toByteArray()

        /*
         * If the XMP is larger than the maximal JPEG segment size (around 65 kb),
         * we need to write multiple APP1 segments with XMP. Most XMP is around
         * 10 to 30 kb, so this is really seldom needed.
         */

        if (xmpXmlBytes.size <= JpegConstants.MAX_SEGMENT_SIZE) {

            val segmentWriter = ByteArrayByteWriter()
            segmentWriter.write(JpegConstants.XMP_IDENTIFIER)
            segmentWriter.write(xmpXmlBytes)

            val segmentData = segmentWriter.toByteArray()

            newPieces.add(UnknownSegment(JpegConstants.JPEG_APP1_MARKER, segmentData))

        } else {

            var offset = 0

            while (offset < xmpXmlBytes.size) {

                val segmentSize = minOf(xmpXmlBytes.size, JpegConstants.MAX_SEGMENT_SIZE)

                val bytesToWrite =
                    xmpXmlBytes.slice(offset until segmentSize).toByteArray()

                val segmentWriter = ByteArrayByteWriter()
                segmentWriter.write(JpegConstants.XMP_IDENTIFIER)
                segmentWriter.write(bytesToWrite)

                val segmentData = segmentWriter.toByteArray()

                newPieces.add(UnknownSegment(JpegConstants.JPEG_APP1_MARKER, segmentData))

                offset += segmentSize
            }
        }

        val mergedPieces = insertAfterLastAppSegments(piecesWithoutXmpSegments, newPieces)

        byteWriter.write(JpegConstants.SOI)

        for (piece in mergedPieces)
            piece.write(byteWriter, byteOrder)
    }
}
