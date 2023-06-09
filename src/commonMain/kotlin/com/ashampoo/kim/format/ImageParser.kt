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
package com.ashampoo.kim.format

import com.ashampoo.kim.common.BinaryFileParser
import com.ashampoo.kim.format.jpeg.JpegImageParser
import com.ashampoo.kim.format.png.PngImageParser
import com.ashampoo.kim.format.tiff.TiffImageParser
import com.ashampoo.kim.input.ByteReader
import com.ashampoo.kim.model.ImageFormat

abstract class ImageParser : BinaryFileParser() {

    abstract fun parseMetadata(byteReader: ByteReader): ImageMetadata

    companion object {

        fun forFormat(imageFormat: ImageFormat): ImageParser? =
            when (imageFormat) {
                ImageFormat.JPEG -> JpegImageParser
                ImageFormat.PNG -> PngImageParser
                ImageFormat.TIFF -> TiffImageParser()
                else -> null
            }
    }
}
