package com.hardbug.photodelete

import com.hardbug.photodelete.models.GalleryPhoto

expect class GalleryRepository {
    suspend fun loadPhotos(): List<GalleryPhoto>
    suspend fun deletePhoto(photo: GalleryPhoto): Boolean
}
