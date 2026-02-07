package com.hardbug.photodelete

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.hardbug.photodelete.ui.GalleryScreen
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val permissionResult = MutableStateFlow<Boolean?>(null)
    private lateinit var permissionsHelper: PermissionsHelper

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionResult.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        permissionsHelper = PermissionsHelper(
            context = applicationContext,
            requestPermission = { requestPermissionLauncher.launch(permission) },
            permissionResult = permissionResult
        )

        val repository = GalleryRepository(applicationContext)

        setContent {
            MaterialTheme {
                GalleryScreen(
                    permissionsHelper = permissionsHelper,
                    repository = repository,
                    onDeleteRequest = { photo ->
                        deletePhotoWithPermission(photo.uri)
                    }
                )
            }
        }
    }

    private fun deletePhotoWithPermission(uriString: String?) {
        if (uriString == null) return
        val uri = Uri.parse(uriString)
        val contentResolver = applicationContext.contentResolver

        try {
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) {
            }
        } catch (e: SecurityException) {
            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    (e as? android.app.RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                }
                else -> null
            }

            intentSender?.let { sender ->
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            }
        }
    }
}
