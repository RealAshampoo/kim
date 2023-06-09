/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
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
package com.ashampoo.kim.model

/**
 * Represents possible updates that can be performed.
 */
sealed interface MetadataUpdate {

    /** In a perfect world every file has an orientation flag. So we don't want NULLs here. */
    data class Orientation(val tiffOrientation: TiffOrientation) : MetadataUpdate

    /** New taken date in millis or NULL to remove it (if the date is wrong and/or not known). */
    data class TakenDate(val takenDate: Long?) : MetadataUpdate

    /** New GPS coordinates or NULL to remove it (if the location is wrong and/or not known) */
    data class GpsCoordinates(val gpsCoordinates: com.ashampoo.kim.model.GpsCoordinates?) : MetadataUpdate

    /** Can't be NULL. Should be UNRATED instead. */
    data class Rating(val photoRating: PhotoRating) : MetadataUpdate

    /** List of new keywords to set. An empty list removes all keywords. */
    data class Keywords(val keywords: Set<String>) : MetadataUpdate

//    /**
//     * List of new faces to set. An empty map removes all faces.
//     * *Note*: Not supported right now!
//     */
//    data class Faces(val faces: Map<String, RegionArea>) : MetadataUpdate

    /** List of new faces to set. An empty list removes all faces. */
    data class Persons(val personsInImage: Set<String>) : MetadataUpdate

}
