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
import com.ashampoo.kim.format.jpeg.elements.GenericSegment.Companion.readSegmentBytes
import com.ashampoo.kim.input.ByteReader

open class AppnSegment(override val marker: Int, override val segmentBytes: ByteArray) :
    Segment(), GenericSegment {
    override val description: String
        get() = "APPN (APP" + (marker - JpegConstants.JPEG_APP0_MARKER) + ") (" + marker + ")"

    constructor(marker: Int, segmentLength: Int, byteReader: ByteReader) :
        this(marker, byteReader.readSegmentBytes(segmentLength))

}
