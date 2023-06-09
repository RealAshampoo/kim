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

import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.decompress
import com.ashampoo.kim.common.indexOfNullTerminator
import com.ashampoo.kim.format.png.ChunkType
import com.ashampoo.kim.format.png.PngConstants
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String

class PngChunkItxt(
    length: Int,
    chunkType: ChunkType,
    crc: Int,
    bytes: ByteArray
) : PngTextChunk(length, chunkType, crc, bytes) {

    @kotlin.jvm.JvmField
    val keyword: String

    @kotlin.jvm.JvmField
    var text: String

    val languageTag: String

    val translatedKeyword: String

    init {

        var terminatorIndex = bytes.indexOfNullTerminator()

        if (terminatorIndex < 0)
            throw ImageReadException("PNG iTXt chunk keyword is not terminated.")

        keyword = String(bytes, 0, terminatorIndex, Charsets.ISO_8859_1)

        var index = terminatorIndex + 1

        val compressionFlag = bytes[index++].toInt()

        if (compressionFlag != 0 && compressionFlag != 1)
            throw ImageReadException("PNG iTXt chunk has invalid compression flag: $compressionFlag")

        val compressed = compressionFlag == 1

        val compressionMethod = bytes[index++].toInt()

        if (compressed && compressionMethod != PngConstants.COMPRESSION_DEFLATE_INFLATE)
            throw ImageReadException("PNG iTXt chunk has unexpected compression method: $compressionMethod")

        terminatorIndex = bytes.indexOfNullTerminator(index)

        if (terminatorIndex < 0)
            throw ImageReadException("PNG iTXt chunk language tag is not terminated.")

        languageTag = String(bytes, index, terminatorIndex - index, Charsets.ISO_8859_1)

        index = terminatorIndex + 1

        terminatorIndex = bytes.indexOfNullTerminator(index)

        if (terminatorIndex < 0)
            throw ImageReadException("PNG iTXt chunk translated keyword is not terminated.")

        translatedKeyword = String(bytes, index, terminatorIndex - index, Charsets.UTF_8)

        index = terminatorIndex + 1

        text = if (compressed)
            decompress(bytes.copyOfRange(index, bytes.size))
        else
            String(bytes, index, bytes.size - index, Charsets.UTF_8)
    }

    /**
     * @return Returns the keyword.
     */
    override fun getKeyword(): String =
        keyword

    /**
     * @return Returns the text.
     */
    override fun getText(): String =
        text
}
