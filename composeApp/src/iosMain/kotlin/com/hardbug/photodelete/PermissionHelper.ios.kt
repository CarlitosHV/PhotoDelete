package com.hardbug.photodelete

import com.hardbug.photodelete.enums.GalleryPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

actual class PermissionsHelper {
    actual fun hasPermission(permission: GalleryPermission): Boolean {
        return when (permission) {
            GalleryPermission.READ -> {
                PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusAuthorized
            }
        }
    }

    actual suspend fun requestPermission(permission: GalleryPermission): Boolean {
        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorization { status ->
                continuation.resume(status == PHAuthorizationStatusAuthorized)
            }
        }
    }
}