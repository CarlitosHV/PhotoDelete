package com.hardbug.photodelete

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.hardbug.photodelete.models.GalleryPhoto
import java.io.File

actual class GalleryRepository(private val context: Context) {
    actual suspend fun loadPhotos(): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                photos.add(GalleryPhoto(
                    id = id.toString(),
                    uri = contentUri.toString(),
                    thumbnailUri = contentUri.toString()
                ))
            }
        }

        return photos
    }

    actual suspend fun deletePhoto(photo: GalleryPhoto): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photo.id.toLong())
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
}
