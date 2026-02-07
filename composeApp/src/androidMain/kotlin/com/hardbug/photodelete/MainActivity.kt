package com.hardbug.photodelete

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import com.hardbug.photodelete.ui.GalleryScreen
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val permissionResult = MutableStateFlow<Boolean?>(null)
    private lateinit var permissionsHelper: PermissionsHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionResult.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
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
                    repository = repository
                )
            }
        }
    }
}
