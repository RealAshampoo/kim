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
package com.ashampoo.kim.format.jpeg.elements

import com.ashampoo.kim.format.jpeg.JpegConstants
import com.ashampoo.kim.format.jpeg.iptc.IptcMetadata
import com.ashampoo.kim.format.jpeg.iptc.IptcParser
import com.ashampoo.kim.input.ByteReader

class App13Segment : AppnSegment {
    override val marker: Int = JpegConstants.JPEG_APP13_MARKER
    constructor(segmentBytes: ByteArray) : super(JpegConstants.JPEG_APP13_MARKER, segmentBytes)
    constructor(segmentLength: Int, byteReader: ByteReader) : super(
        JpegConstants.JPEG_APP13_MARKER,
        segmentLength,
        byteReader
    )

    fun isPhotoshopJpegSegment(): Boolean =
        IptcParser.isIptcSegment(segmentBytes)

    fun parseIptcMetadata(): IptcMetadata? {

        /*
         * In practice, App13 segments are only used for Photoshop/IPTC
         * metadata. However, we should not treat App13 signatures without
         * Photoshop's signature as Photoshop/IPTC segments.
         */
        if (!isPhotoshopJpegSegment())
            return null

        return IptcParser.parseIptc(segmentBytes)
    }
}
