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
package com.ashampoo.kim.format.png.chunks

import com.ashampoo.kim.common.BinaryFileParser
import com.ashampoo.kim.format.png.ChunkType

open class PngChunk(
    val length: Int,
    val chunkType: ChunkType,
    val crc: Int,
    val bytes: ByteArray
) : BinaryFileParser() {

    val ancillary: Boolean
    val isPrivate: Boolean
    val reserved: Boolean
    val safeToCopy: Boolean

    init {

        val propertyBits = BooleanArray(4)

        var shift = 24

        for (i in 0..3) {

            val theByte = 0xFF and (chunkType.intValue shr shift)

            shift -= 8

            val theMask = 1 shl 5
            propertyBits[i] = theByte and theMask > 0
        }

        ancillary = propertyBits[0]
        isPrivate = propertyBits[1]
        reserved = propertyBits[2]
        safeToCopy = propertyBits[3]
    }

    override fun toString() =
        "PngChunk " + chunkType.name + " (" + length + ")"
}
