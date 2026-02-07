package com.hardbug.photodelete

import com.hardbug.photodelete.models.GalleryPhoto
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSSortDescriptor
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class GalleryRepository {

    actual suspend fun loadPhotos(): List<GalleryPhoto> = suspendCoroutine { continuation ->
        val photos = mutableListOf<GalleryPhoto>()

        val fetchOptions = PHFetchOptions().apply {
            sortDescriptors = listOf(
                NSSortDescriptor(key = "creationDate", ascending = false)
            )
        }

        val fetchResult = PHAsset.fetchAssetsWithMediaType(
            PHAssetMediaTypeImage,
            fetchOptions
        )

        val dateFormatter = NSDateFormatter().apply {
            dateFormat = "dd/MM/yyyy"
        }

        for (i in 0 until fetchResult.count.toInt()) {
            val asset = fetchResult.objectAtIndex(i.toULong()) as PHAsset
            val localIdentifier = asset.localIdentifier
            val creationDate = asset.creationDate?.let { dateFormatter.stringFromDate(it) }

            photos.add(GalleryPhoto(
                id = localIdentifier,
                uri = localIdentifier,
                thumbnailUri = localIdentifier,
                creationDate = creationDate
            ))
        }

        continuation.resume(photos)
    }

    actual suspend fun deletePhoto(photo: GalleryPhoto): Boolean = suspendCoroutine { continuation ->
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(
            listOf(photo.id),
            null
        )

        if (fetchResult.count.toInt() == 0) {
            continuation.resume(false)
            return@suspendCoroutine
        }

        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            {
                PHAssetChangeRequest.deleteAssets(fetchResult)
            },
            completionHandler = { success, error ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        )
    }
}
