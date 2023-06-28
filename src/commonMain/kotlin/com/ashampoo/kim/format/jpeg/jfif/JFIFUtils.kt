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
package com.ashampoo.kim.format.jpeg.jfif

import com.ashampoo.kim.common.ByteOrder
import com.ashampoo.kim.common.startsWith
import com.ashampoo.kim.common.toBytes
import com.ashampoo.kim.format.jpeg.JpegConstants
import com.ashampoo.kim.format.jpeg.elements.JpegBytesElement
import com.ashampoo.kim.format.jpeg.elements.JpegSOS
import com.ashampoo.kim.format.jpeg.elements.UnknownSegment
import com.ashampoo.kim.format.jpeg.iptc.IptcParser
import com.ashampoo.kim.output.ByteWriter

object JFIFUtils {
    fun JpegBytesElement.write(byteWriter: ByteWriter, byteOrder: ByteOrder) {
        val markerBytes = marker.toShort().toBytes(byteOrder)
        when (this) {
            is JpegSOS -> {
                byteWriter.write(markerBytes)
                byteWriter.write(imageData)
            }
            is UnknownSegment -> {
                byteWriter.write(markerBytes)
                byteWriter.write((segmentBytes.size + 2).toShort().toBytes(byteOrder))
                byteWriter.write(segmentBytes)
            }
        }
    }

    fun UnknownSegment.isAppSegment(): Boolean =
        marker >= JpegConstants.JPEG_APP0_MARKER && marker <= JpegConstants.JPEG_APP15_MARKER

    fun UnknownSegment.isExifSegment(): Boolean {

        if (marker != JpegConstants.JPEG_APP1_MARKER)
            return false

        return segmentBytes.startsWith(JpegConstants.EXIF_IDENTIFIER_CODE)
    }

    fun UnknownSegment.isIptcSegment(): Boolean {

        if (marker != JpegConstants.JPEG_APP13_MARKER)
            return false

        return IptcParser.isIptcSegment(segmentBytes)
    }

    fun UnknownSegment.isXmpSegment(): Boolean {

        if (marker != JpegConstants.JPEG_APP1_MARKER)
            return false

        return segmentBytes.startsWith(JpegConstants.XMP_IDENTIFIER)
    }
}
