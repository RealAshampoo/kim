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

import com.ashampoo.kim.input.ByteReader

sealed interface JpegElement {
    val marker: Int
}

sealed interface JpegBytesElement : JpegElement {
    val bytes: ByteArray
}

sealed interface JpegSegment : JpegElement {
    val description: String
}

class JpegSOS(
    override val marker: Int,
    private val remainingBytes: ByteReader
) : JpegBytesElement {
    val imageData: ByteArray
        get() = remainingBytes.readRemainingBytes()
    override val bytes: ByteArray get() = imageData

    operator fun component1() = marker
    operator fun component2() = imageData
}
