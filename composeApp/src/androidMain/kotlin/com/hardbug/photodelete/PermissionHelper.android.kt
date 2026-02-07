package com.hardbug.photodelete

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.hardbug.photodelete.enums.GalleryPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

actual class PermissionsHelper(
    private val context: Context,
    private val requestPermission: () -> Unit,
    private val permissionResult: MutableStateFlow<Boolean?>
) {
    actual fun hasPermission(permission: GalleryPermission): Boolean {
        return when (permission) {
            GalleryPermission.READ -> {
                val permissionStr = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                ContextCompat.checkSelfPermission(
                    context,
                    permissionStr
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    actual suspend fun requestPermission(permission: GalleryPermission): Boolean {
        if (hasPermission(permission)) return true

        permissionResult.value = null
        requestPermission()

        return permissionResult.first { it != null } ?: false
    }
}
