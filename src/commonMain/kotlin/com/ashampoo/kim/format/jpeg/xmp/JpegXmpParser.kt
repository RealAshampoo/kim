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
package com.ashampoo.kim.format.jpeg.xmp

import com.ashampoo.kim.common.BinaryFileParser
import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.startsWith
import com.ashampoo.kim.format.jpeg.JpegConstants
import com.ashampoo.kim.format.jpeg.JpegConstants.JPEG_BYTE_ORDER
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String

object JpegXmpParser : BinaryFileParser() {

    init {
        byteOrder = JPEG_BYTE_ORDER
    }

    fun isXmpJpegSegment(segmentData: ByteArray): Boolean =
        segmentData.startsWith(JpegConstants.XMP_IDENTIFIER)

    fun parseXmpJpegSegment(segmentData: ByteArray): String {

        if (!isXmpJpegSegment(segmentData))
            throw ImageReadException("Invalid JPEG XMP Segment.")

        val index = JpegConstants.XMP_IDENTIFIER.size

        /* The data is UTF-8 encoded XML */
        return String(segmentData, index, segmentData.size - index, Charsets.UTF_8)
    }
}
