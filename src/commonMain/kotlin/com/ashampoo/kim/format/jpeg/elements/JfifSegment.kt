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

import com.ashampoo.kim.common.ByteOrder
import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.format.jpeg.JpegConstants
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.input.ByteReader

data class JfifSegment(
    val jfifMajorVersion: Int,
    val jfifMinorVersion: Int,
    val densityUnits: Int,
    val xDensity: Int,
    val yDensity: Int,
    val xThumbnail: Int,
    val yThumbnail: Int,
) : Segment() {
    override val marker: Int = JpegConstants.JFIF_MARKER
    val thumbnailSize: Int get() = xThumbnail * yThumbnail

    override val description: String get() = "JFIF ($marker)"

    constructor(byteReader: ByteReader, byteOrder: ByteOrder) : this(
        byteReader.readBytes("Unexpected EOF", JpegConstants.JFIF0_SIGNATURE.size).let { signature ->
            if (JpegConstants.JFIF0_SIGNATURE != signature &&
                JpegConstants.JFIF0_SIGNATURE_ALTERNATIVE != signature
            )
                throw ImageReadException("Not a Valid JPEG File: missing JFIF string")
            byteReader.readByte("JFIF major version").toInt()
        },
        jfifMinorVersion = byteReader.readByte("JFIF minor version").toInt(),
        densityUnits = byteReader.readByte("density units").toInt(),
        xDensity = byteReader.read2BytesAsInt("xDensity", byteOrder),
        yDensity = byteReader.read2BytesAsInt("yDensity", byteOrder),
        xThumbnail = byteReader.readByte("xThumbnail").toInt(),
        yThumbnail = byteReader.readByte("yThumbnail").toInt(),
    ) {
        if (thumbnailSize > 0)
            byteReader.skipBytes("Missing thumbnail", thumbnailSize.toLong())
    }

    constructor(segmentBytes: ByteArray, byteOrder: ByteOrder) :
        this(ByteArrayByteReader(segmentBytes), byteOrder)

}
