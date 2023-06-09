/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
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

import com.ashampoo.kim.common.ByteOrder
import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.toSingleNumberHexes
import com.ashampoo.kim.common.toUInt16
import com.ashampoo.kim.format.ImageFormatMagicNumbers
import com.ashampoo.kim.input.ByteReader

object JpegMetadataExtractor {

    private const val SEGMENT_IDENTIFIER = 0xFF.toByte()
    private const val SEGMENT_START_OF_SCAN = 0xDA.toByte()
    private const val MARKER_END_OF_IMAGE = 0xD9.toByte()

    private const val ADDITIONAL_BYTE_COUNT_AFTER_HEADER: Int = 12

    @Suppress("ComplexMethod")
    fun extractMetadataBytes(reader: ByteReader): ByteArray {

        val bytes = mutableListOf<Byte>()

        val magicNumberBytes = reader.readBytes(ImageFormatMagicNumbers.jpegShort.size).toList()

        /* Ensure it's actually a JPEG. */
        require(magicNumberBytes == ImageFormatMagicNumbers.jpegShort) {
            "JPEG magic number mismatch: ${magicNumberBytes.toByteArray().toSingleNumberHexes()}"
        }

        bytes.addAll(magicNumberBytes)

        @Suppress("LoopWithTooManyJumpStatements")
        do {

            var segmentIdentifier = reader.readByte() ?: break
            var segmentType = reader.readByte() ?: break

            bytes.add(segmentIdentifier)
            bytes.add(segmentType)

            /*
             * Find the segment marker. Markers are zero or more 0xFF bytes, followed by
             * a 0xFF and then a byte not equal to 0x00 or 0xFF.
             */
            while (
                segmentIdentifier != SEGMENT_IDENTIFIER ||
                segmentType == SEGMENT_IDENTIFIER ||
                segmentType.toInt() == 0
            ) {

                segmentIdentifier = segmentType

                val nextSegmentType = reader.readByte() ?: break

                bytes.add(nextSegmentType)

                segmentType = nextSegmentType
            }

            if (segmentType == SEGMENT_START_OF_SCAN || segmentType == MARKER_END_OF_IMAGE)
                break

            val segmentLengthFirstByte = reader.readByte() ?: break
            val segmentLengthSecondByte = reader.readByte() ?: break

            bytes.add(segmentLengthFirstByte)
            bytes.add(segmentLengthSecondByte)

            /* Next 2-bytes are <segment-size>: [high-byte] [low-byte] */
            var segmentLength: Int = byteArrayOf(segmentLengthFirstByte, segmentLengthSecondByte)
                .toUInt16(ByteOrder.BIG_ENDIAN)

            /* Segment length includes size bytes, so subtract two */
            segmentLength -= 2

            if (segmentLength <= 0)
                throw ImageReadException("Illegal JPEG segment length: $segmentLength")

            val segmentBytes = reader.readBytes(segmentLength)

            if (segmentBytes.size != segmentLength)
                throw ImageReadException("Incomplete read: ${segmentBytes.size} != $segmentLength")

            bytes.addAll(segmentBytes.asList())

        } while (true)

        /**
         * Add some more bytes after the header, so it's recognized
         * by most image viewers as a valid (but broken) file.
         */
        repeat(ADDITIONAL_BYTE_COUNT_AFTER_HEADER) {

            reader.readByte()?.let {
                bytes.add(it)
            }
        }

        return bytes.toByteArray()
    }
}
