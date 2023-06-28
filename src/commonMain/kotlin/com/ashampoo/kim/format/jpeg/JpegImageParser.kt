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

import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.getRemainingBytes
import com.ashampoo.kim.common.startsWith
import com.ashampoo.kim.format.ImageMetadata
import com.ashampoo.kim.format.ImageParser
import com.ashampoo.kim.format.jpeg.JpegConstants.JPEG_BYTE_ORDER
import com.ashampoo.kim.format.jpeg.elements.App13Segment
import com.ashampoo.kim.format.jpeg.elements.AppnSegment
import com.ashampoo.kim.format.jpeg.elements.ComSegment
import com.ashampoo.kim.format.jpeg.elements.GenericSegment
import com.ashampoo.kim.format.jpeg.elements.JfifSegment
import com.ashampoo.kim.format.jpeg.elements.JpegSegment
import com.ashampoo.kim.format.jpeg.elements.SofnSegment
import com.ashampoo.kim.format.jpeg.elements.UnknownSegment
import com.ashampoo.kim.format.jpeg.iptc.IptcMetadata
import com.ashampoo.kim.format.jpeg.xmp.JpegXmpParser
import com.ashampoo.kim.format.tiff.TiffContents
import com.ashampoo.kim.format.tiff.TiffReader
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.input.ByteReader
import com.ashampoo.kim.model.ImageFormat
import com.ashampoo.kim.model.ImageSize

object JpegImageParser : ImageParser() {

    init {
        byteOrder = JPEG_BYTE_ORDER
    }

    override fun parseMetadata(byteReader: ByteReader): ImageMetadata {

        val segments = readSegments(
            byteReader,
            JpegConstants.SOFN_MARKERS +
                listOf(JpegConstants.JPEG_APP1_MARKER, JpegConstants.JPEG_APP13_MARKER)
        )

        val imageSize = getImageSize(segments)

        val exif = getExif(segments)

        val iptc = getIptc(segments)

        val xmp = getXmpXml(segments)

        return ImageMetadata(ImageFormat.JPEG, imageSize, exif, iptc, xmp)
    }

    private fun readSegments(byteReader: ByteReader, markers: List<Int>): List<JpegSegment> =
        JpegUtils.readJFIF(byteReader)
            .filterIsInstance<UnknownSegment>()
            .mapNotNull { (marker, segmentBytes) ->
                when (marker) {
                    !in markers -> null
                    JpegConstants.JPEG_APP1_MARKER -> AppnSegment(marker, segmentBytes)
                    JpegConstants.JPEG_APP13_MARKER -> App13Segment(segmentBytes)
                    JpegConstants.JFIF_MARKER -> JfifSegment(segmentBytes, byteOrder)

                    in JpegConstants.SOFN_MARKERS -> SofnSegment(marker, segmentBytes, byteOrder)

                    in JpegConstants.JPEG_APP1_MARKER..JpegConstants.JPEG_APP15_MARKER ->
                        UnknownSegment(marker, segmentBytes)

                    JpegConstants.COM_MARKER -> ComSegment(marker, segmentBytes)
                    else -> null
                }
            }
            .toList()

    private fun getImageSize(segments: List<JpegSegment>): ImageSize {

        val sofnSegment = segments.filterIsInstance<SofnSegment>()

        if (sofnSegment.isEmpty())
            throw ImageReadException("No JFIF Data Found.")

        if (sofnSegment.size > 1)
            throw ImageReadException("Redundant JFIF Data Found.")

        val firstSegment = sofnSegment.first()

        return ImageSize(firstSegment.width, firstSegment.height)
    }

    private fun getExif(segments: List<JpegSegment>): TiffContents? {

        val bytes = getExifBytes(segments) ?: return null

        val exifByteReader = ByteArrayByteReader(bytes)

        val contents = TiffReader().read(exifByteReader)

        return contents
    }

    private fun getExifBytes(segments: List<JpegSegment>): ByteArray? {

        val exifSegments = segments
            .filterIsInstance<GenericSegment>()
            .filter { it.segmentBytes.startsWith(JpegConstants.EXIF_IDENTIFIER_CODE) }

        if (exifSegments.isEmpty())
            return null

        if (exifSegments.size > 1)
            throw ImageReadException("Multiple APP1 EXIF segments are unsupported right now.")

        val firstSegment = exifSegments.first()

        return firstSegment.segmentBytes.getRemainingBytes(6)
    }

    private fun getXmpXml(segments: List<JpegSegment>): String? {

        val xmpSegments = segments
            .filterIsInstance<AppnSegment>()
            .filter { segment -> JpegXmpParser.isXmpJpegSegment(segment.segmentBytes) }

        if (xmpSegments.isEmpty())
            return null

        /*
         * Some files in our test repo have multiple XMP strings.
         * This seems to be an error, because it's the same content, but only formatted.
         * We do here what ExifTool does on "exiftool -xmp -b photo.jpg > photo.xmp"
         * and take the first by ignoring the rest.
         */

        return JpegXmpParser.parseXmpJpegSegment(xmpSegments.first().segmentBytes)
    }

    private fun getIptc(segments: List<JpegSegment>): IptcMetadata? {

        val app13Segments = segments.filterIsInstance<App13Segment>()
        var iptcMetadata: IptcMetadata? = null

        for (segment in app13Segments) {

            iptcMetadata = try {
                val metadata = segment.parseIptcMetadata()
                /*
                 * In case of multiple APP13 segements the records should be merged.
                 * It's now allowed to have multiple ones, if it does not fit into a single one.
                 */
                if (iptcMetadata != null && metadata != null)
                    iptcMetadata + metadata
                else
                    metadata
            } catch (ignore: ImageReadException) {
                println("Ignored broken IPTC: ${ignore.message}")
                continue
            }
        }

        return null
    }
}
