package com.hardbug.photodelete.models

data class GalleryPhoto(
    val id: String,
    val uri: String?,
    val thumbnailUri: String?,
    val creationDate: String?,
    val deviceModel: String? = null,
    val location: String? = null,
    val cameraSpecs: String? = null
)

