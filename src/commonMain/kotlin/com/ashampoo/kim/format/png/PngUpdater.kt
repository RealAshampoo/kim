/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.ashampoo.kim.format.png

import com.ashampoo.kim.Kim
import com.ashampoo.kim.common.ImageWriteException
import com.ashampoo.kim.model.ImageFormat
import com.ashampoo.kim.model.MetadataUpdate

internal object PngUpdater {

    fun update(
        bytes: ByteArray,
        updates: Set<MetadataUpdate>
    ): ByteArray {

        val kimMetadata = Kim.readMetadata(bytes)

        if (kimMetadata == null)
            throw ImageWriteException("Could not read file.")

        if (kimMetadata.imageFormat != ImageFormat.PNG)
            throw ImageWriteException("Can only update PNG.")

        /*
         * TODO Implement
         */

        return bytes
    }
}