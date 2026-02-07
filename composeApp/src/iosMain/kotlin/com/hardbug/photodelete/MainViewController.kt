package com.hardbug.photodelete

import androidx.compose.ui.window.ComposeUIViewController
import com.hardbug.photodelete.ui.GalleryScreen

fun MainViewController() = ComposeUIViewController {
    val permissionsHelper = PermissionsHelper()
    val repository = GalleryRepository()

    GalleryScreen(
        permissionsHelper = permissionsHelper,
        repository = repository
    )
}