package com.odom.photocard.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.odom.photocard.viewmodel.PhotoCardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Screen for editing background image (crop and rotate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditScreen(
    viewModel: PhotoCardViewModel,
    onNavigateBack: () -> Unit,
    onEditComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isCropping by remember { mutableStateOf(false) }
    
    // Load the image
    LaunchedEffect(state.imageUri) {
        state.imageUri?.let { uri ->
            imageBitmap = loadBitmapFromUri(context, uri)?.asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val editedUri = saveEditedImage(
                                    context,
                                    imageBitmap?.asAndroidBitmap(),
                                    rotation,
                                    scale,
                                    offset
                                )
                                editedUri?.let { uri ->
                                    viewModel.setImage(uri, context)
                                    onEditComplete()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Image editing area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clipToBounds()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                imageBitmap?.let { bitmap ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (isCropping) {
                                        offset = Offset(
                                            offset.x + pan.x,
                                            offset.y + pan.y
                                        )
                                        scale *= zoom
                                    }
                                }
                            }
                    ) {
                        drawIntoCanvas { canvas ->
                            val androidCanvas = canvas.nativeCanvas
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            
                            androidCanvas.save()
                            
                            // Apply transformations
                            androidCanvas.translate(centerX + offset.x, centerY + offset.y)
                            androidCanvas.rotate(rotation)
                            androidCanvas.scale(scale, scale)
                            
                            // Draw the bitmap centered
                            val bitmapWidth = bitmap.width.toFloat()
                            val bitmapHeight = bitmap.height.toFloat()
                            androidCanvas.drawBitmap(
                                bitmap.asAndroidBitmap(),
                                -bitmapWidth / 2,
                                -bitmapHeight / 2,
                                null
                            )
                            
                            androidCanvas.restore()
                        }
                    }
                    
                    // Crop overlay
                    if (isCropping) {
                        CropOverlay()
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledIconButton(
                    onClick = { rotation += 90f },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Rotate90DegreesCw,
                        contentDescription = "Rotate 90°"
                    )
                }
                
                FilledIconButton(
                    onClick = { rotation += 180f },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Rotate 180°"
                    )
                }
                
                FilledIconButton(
                    onClick = { 
                        isCropping = !isCropping
                        if (!isCropping) {
                            // Reset crop adjustments when exiting crop mode
                            scale = 1f
                            offset = Offset.Zero
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = if (isCropping) "Cancel Crop" else "Crop"
                    )
                }
            }
            
            // Instructions
            Text(
                text = if (isCropping) {
                    "Pinch to zoom, drag to move. Tap crop button when done."
                } else {
                    "Tap rotate buttons to rotate image. Tap crop to enable cropping."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CropOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // Center crop area (clear rectangle)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .background(Color.Transparent)
        ) {
            // Draw crop border
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                drawRect(
                    color = Color.White,
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
                
                // Draw rule of thirds grid
                val thirdWidth = size.width / 3
                val thirdHeight = size.height / 3
                
                // Vertical lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(thirdWidth, 0f),
                    end = Offset(thirdWidth, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(thirdWidth * 2, 0f),
                    end = Offset(thirdWidth * 2, size.height),
                    strokeWidth = 1f
                )
                
                // Horizontal lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, thirdHeight),
                    end = Offset(size.width, thirdHeight),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, thirdHeight * 2),
                    end = Offset(size.width, thirdHeight * 2),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
            
            val result = coil3.ImageLoader(context).execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun saveEditedImage(
    context: Context,
    bitmap: Bitmap?,
    rotation: Float,
    scale: Float,
    offset: Offset
): Uri? {
    if (bitmap == null) return null
    
    return withContext(Dispatchers.IO) {
        try {
            // Create matrix for transformations
            val matrix = Matrix()
            
            // Apply rotation
            matrix.postRotate(rotation % 360)
            
            // Apply scale
            matrix.postScale(scale, scale)
            
            // Create transformed bitmap
            val transformedBitmap = Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.width, bitmap.height,
                matrix,
                true
            )
            
            // Save to file
            val file = File(context.cacheDir, "edited_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                transformedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
