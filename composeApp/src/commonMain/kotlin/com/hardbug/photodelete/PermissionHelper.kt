package com.hardbug.photodelete

import com.hardbug.photodelete.enums.GalleryPermission

expect class PermissionsHelper {
    suspend fun requestPermission(permission: GalleryPermission): Boolean
    fun hasPermission(permission: GalleryPermission): Boolean
}