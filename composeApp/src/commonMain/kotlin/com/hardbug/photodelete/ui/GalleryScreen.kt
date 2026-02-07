package com.hardbug.photodelete.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hardbug.photodelete.GalleryRepository
import com.hardbug.photodelete.PermissionsHelper
import com.hardbug.photodelete.enums.GalleryPermission
import com.hardbug.photodelete.models.GalleryPhoto
import kotlinx.coroutines.launch

@Composable
fun GalleryScreen(
    permissionsHelper: PermissionsHelper,
    repository: GalleryRepository
) {
    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hasPermission = permissionsHelper.hasPermission(GalleryPermission.READ)

        if (hasPermission) {
            photos = repository.loadPhotos()
            isLoading = false
        } else {
            isLoading = false
            showPermissionRationale = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            showPermissionRationale -> {
                PermissionRationaleDialog(
                    onRequestPermission = {
                        scope.launch {
                            val granted = permissionsHelper.requestPermission(GalleryPermission.READ)
                            if (granted) {
                                hasPermission = true
                                showPermissionRationale = false
                                isLoading = true
                                photos = repository.loadPhotos()
                                isLoading = false
                            }
                        }
                    },
                    onDismiss = {
                        showPermissionRationale = false
                    }
                )
            }

            photos.isEmpty() -> {
                Text("No hay fotos en la galería")
            }

            else -> {
                PhotoViewer(
                    photos = photos,
                    repository = repository
                )
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permiso requerido") },
        text = {
            Text("Esta app necesita acceso a tu galería para mostrar y gestionar tus fotos.")
        },
        confirmButton = {
            TextButton(onClick = onRequestPermission) {
                Text("Permitir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
