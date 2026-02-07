package com.hardbug.photodelete

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.hardbug.photodelete.models.GalleryPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual class GalleryRepository(private val context: Context) {
    actual suspend fun loadPhotos(): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(Date(dateAdded * 1000))

                val (device, location, specs) = getExifData(context, contentUri)

                photos.add(GalleryPhoto(
                    id = id.toString(),
                    uri = contentUri.toString(),
                    thumbnailUri = contentUri.toString(),
                    creationDate = formattedDate,
                    deviceModel = device,
                    location = location,
                    cameraSpecs = specs
                ))
            }
        }
        return photos
    }

    private fun getExifData(context: Context, uri: Uri): Triple<String, String, String> {
        var device = "Desconocido"
        var location = "Sin ubicación"
        var specs = "--"

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)

                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                if (model.isNotEmpty()) device = "$make $model".trim()

                val fObj = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                if (fObj != null) specs = "ƒ/$fObj ISO ${iso ?: "?"}"

                val latLong = exif.latLong
                if (latLong != null) {
                    location = String.format("%.4f, %.4f", latLong[0], latLong[1])
                }
            }
        } catch (e: Exception) {
        }
        return Triple(device, location, specs)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    actual suspend fun deletePhoto(photo: GalleryPhoto): Boolean {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photo.id.toLong()
        )

        return try {
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: SecurityException) {
            val recoverableSecurityException = e as? android.app.RecoverableSecurityException
            if (recoverableSecurityException != null) {
                throw recoverableSecurityException
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                throw DeletePermissionException(uri)
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    class DeletePermissionException(val uri: android.net.Uri) : Exception()
}
