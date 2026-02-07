package com.hardbug.photodelete.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hardbug.photodelete.GalleryRepository
import com.hardbug.photodelete.models.GalleryPhoto
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragAnchors {
    Collapsed,
    Expanded,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewer(
    photos: List<GalleryPhoto>,
    repository: GalleryRepository,
    onDelete: (GalleryPhoto) -> Unit
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
            FinishedCard { currentIndex = 0 }
        } else {
            val visibleStackSize = 3

            (currentIndex + visibleStackSize downTo currentIndex).forEach { photoIndex ->
                if (photoIndex < photos.size) {
                    val photo = photos[photoIndex]
                    val positionInStack = photoIndex - currentIndex
                    val isFrontCard = positionInStack == 0

                    val animatedState = getAnimatedCardState(positionInStack, isFrontCard, offsetX)

                    PhotoCard(
                        photo = photo,
                        modifier = Modifier
                            .offset {
                                if (isFrontCard) IntOffset(animatedState.offsetX.roundToInt(), 0)
                                else IntOffset(0, 0)
                            }
                            .graphicsLayer {
                                translationY = animatedState.offsetY.toPx()
                                scaleX = animatedState.scale
                                scaleY = animatedState.scale
                                rotationZ = animatedState.rotation
                                alpha = animatedState.alpha
                            },
                        isFrontCard = isFrontCard,
                        swipeOffset = offsetX,
                        onDrag = { deltaX -> offsetX += deltaX },
                        onDragEnd = {
                            scope.launch {
                                val swipeThreshold = 400
                                if (offsetX > swipeThreshold) {
                                    currentIndex++
                                } else if (offsetX < -swipeThreshold) {
                                    onDelete(photo)
                                    currentIndex++
                                }
                                offsetX = 0f
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun getAnimatedCardState(
    positionInStack: Int,
    isFrontCard: Boolean,
    offsetX: Float
): AnimatedCardState {
    val scale by animateFloatAsState(1f - (positionInStack * 0.05f), tween(300), label = "")
    val offsetY by animateDpAsState((positionInStack * 12).dp, tween(300), label = "")
    val rotation by animateFloatAsState(if (isFrontCard) (offsetX / 40f) else 0f, label = "")
    val alpha by animateFloatAsState(if (isFrontCard) 1f - (abs(offsetX) / 600f) else 1f, label = "")

    return remember(scale, offsetY, rotation, alpha) {
        AnimatedCardState(scale, offsetY, rotation, alpha, offsetX)
    }
}

private data class AnimatedCardState(
    val scale: Float,
    val offsetY: Dp,
    val rotation: Float,
    val alpha: Float,
    val offsetX: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCard(
    photo: GalleryPhoto,
    modifier: Modifier = Modifier,
    isFrontCard: Boolean,
    swipeOffset: Float,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val sheetState = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Collapsed,
            anchors = DraggableAnchors { },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    val gestureModifier = if (isFrontCard) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        onDrag(dragAmount.x)
                    } else {
                        scope.launch {
                            sheetState.dispatchRawDelta(dragAmount.y)
                        }
                    }
                },
                onDragEnd = {
                    onDragEnd()
                    scope.launch {
                        sheetState.settle(0f)
                    }
                }
            )
        }
    } else Modifier

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .then(gestureModifier),
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
                SwipeActionOverlay(swipeOffset)
            }

            PhotoMetadataSheet(photo, sheetState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.PhotoMetadataSheet(
    photo: GalleryPhoto,
    state: AnchoredDraggableState<DragAnchors>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .onSizeChanged { layoutSize ->
                val heightPx = layoutSize.height.toFloat()
                val newAnchors = DraggableAnchors {
                    DragAnchors.Collapsed at heightPx
                    DragAnchors.Expanded at 0f
                }
                if (state.anchors.positionOf(DragAnchors.Collapsed).isNaN()) {
                    state.updateAnchors(newAnchors, DragAnchors.Collapsed)
                } else {
                    state.updateAnchors(newAnchors)
                }
            }
            .offset {
                val offset = state.offset
                val y = if (offset.isNaN()) 10000 else offset.roundToInt()
                IntOffset(0, y)
            }
            .anchoredDraggable(state, orientation = Orientation.Vertical)
            .background(
                Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = photo.creationDate ?: "Fecha desconocida",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetadataRow(Icons.Default.Smartphone, "Dispositivo", photo.deviceModel ?: "Desconocido")
        MetadataRow(Icons.Default.LocationOn, "Ubicación", photo.location ?: "Sin ubicación")
        MetadataRow(Icons.Default.CameraAlt, "Cámara", photo.cameraSpecs ?: "--")
    }
}

@Composable
private fun MetadataRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = title, tint = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SwipeActionOverlay(offsetX: Float) {
    val overlayAlpha = (abs(offsetX) / 600f).coerceIn(0f, 0.4f)
    val iconAlpha = (abs(offsetX) / 400f).coerceIn(0f, 1f)

    when {
        offsetX > 10 -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Green.copy(alpha = overlayAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Keep",
                    tint = Color.White.copy(alpha = iconAlpha),
                    modifier = Modifier.size(128.dp)
                )
            }
        }
        offsetX < -10 -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = overlayAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = iconAlpha),
                    modifier = Modifier.size(128.dp)
                )
            }
        }
    }
}

@Composable
fun FinishedCard(onRestart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "¡Todo listo!",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Has revisado todas tus fotos.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRestart) {
                Text("Empezar de nuevo")
            }
        }
    }
}
