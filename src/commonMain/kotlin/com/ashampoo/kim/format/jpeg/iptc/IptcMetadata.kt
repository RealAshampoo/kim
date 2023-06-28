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
package com.ashampoo.kim.format.jpeg.iptc

/**
 * IPTC as located in JPEG APP13 segments
 */
data class IptcMetadata(
    val records: List<IptcRecord>,
    val rawBlocks: List<IptcBlock>
) {

    val nonIptcBlocks: List<IptcBlock> by lazy {
        rawBlocks.filterNot { it.isIPTCBlock() }
    }

    override fun toString(): String {

        val sb = StringBuilder()
        sb.appendLine("---- IPTC ----")

        for (record in records)
            sb.appendLine(record)

        return sb.toString()
    }

    operator fun plus(other: IptcMetadata): IptcMetadata = IptcMetadata(
        records = records + other.records,
        rawBlocks = rawBlocks + other.rawBlocks
    )
}
