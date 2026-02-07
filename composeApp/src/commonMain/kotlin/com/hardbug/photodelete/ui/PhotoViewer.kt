package com.hardbug.photodelete.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hardbug.photodelete.GalleryRepository
import com.hardbug.photodelete.models.GalleryPhoto
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PhotoViewer(
    photos: List<GalleryPhoto>,
    repository: GalleryRepository
) {
    var currentIndex by remember { mutableStateOf(0) }
    var offsetX by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentIndex >= photos.size) {
            Text("¡Todas las fotos revisadas!")
        } else {
            val visibleStackSize = 3

            // Draw cards from back to front
            (currentIndex + visibleStackSize downTo currentIndex).forEach { photoIndex ->
                if (photoIndex < photos.size) {
                    val photo = photos[photoIndex]
                    val positionInStack = photoIndex - currentIndex
                    val isFrontCard = positionInStack == 0

                    val scale by animateFloatAsState(
                        targetValue = 1f - (positionInStack * 0.05f),
                        animationSpec = tween(300),
                        label = "scale"
                    )
                    val offsetY by animateDpAsState(
                        targetValue = (positionInStack * 12).dp,
                        animationSpec = tween(300),
                        label = "offsetY"
                    )
                    val animatedRotation by animateFloatAsState(
                        targetValue = if (isFrontCard) (offsetX / 40f) else 0f,
                        label = "rotation"
                    )
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isFrontCard) 1f - (abs(offsetX) / 600f) else 1f,
                        label = "alpha"
                    )

                    val dragModifier = if (isFrontCard) {
                        Modifier.pointerInput(photo.id) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val swipeThreshold = 400
                                        if (offsetX > swipeThreshold) {
                                            currentIndex++
                                        } else if (offsetX < -swipeThreshold) {
                                            repository.deletePhoto(photo)
                                            currentIndex++
                                        }
                                        offsetX = 0f
                                    }
                                }
                            )
                        }
                    } else Modifier

                    Card(
                        modifier = Modifier
                            .offset {
                                if (isFrontCard) IntOffset(offsetX.roundToInt(), 0)
                                else IntOffset(0, 0)
                            }
                            .graphicsLayer {
                                translationY = offsetY.toPx()
                                scaleX = scale
                                scaleY = scale
                                rotationZ = animatedRotation
                                alpha = if (positionInStack < visibleStackSize) animatedAlpha else 0f
                            }
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .then(dragModifier),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = "Photo ${photo.id}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (isFrontCard) {
                                val overlayAlpha = (abs(offsetX) / 600f).coerceIn(0f, 0.4f)
                                val iconAlpha = (abs(offsetX) / 400f).coerceIn(0f, 1f)

                                when {
                                    offsetX > 10 -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Green.copy(alpha = overlayAlpha))
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Keep",
                                            tint = Color.White.copy(alpha = iconAlpha),
                                            modifier = Modifier.align(Alignment.Center).size(128.dp)
                                        )
                                    }

                                    offsetX < -10 -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Red.copy(alpha = overlayAlpha))
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White.copy(alpha = iconAlpha),
                                            modifier = Modifier.align(Alignment.Center).size(128.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
