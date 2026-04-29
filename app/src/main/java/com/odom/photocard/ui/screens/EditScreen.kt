package com.odom.photocard.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.odom.photocard.viewmodel.PhotoCardViewModel
import com.odom.photocard.viewmodel.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Screen for editing photo card with text overlays
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: PhotoCardViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Show toast for save success
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo Card") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                saveAndShareImage(context, state, viewModel)
                            }
                        },
                        enabled = !state.isSaving
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addTextOverlay() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Text")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Image display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Show editor if we have either URI or bitmap
                if (state.imageUri != null || state.bitmap != null) {
                    PhotoCardEditor(
                        imageUri = state.imageUri,
                        bitmap = state.bitmap,
                        textOverlays = state.textOverlays,
                        selectedTextId = state.selectedTextId,
                        onTextSelected = { viewModel.selectTextOverlay(it) },
                        onTextMoved = { id, x, y ->
                            viewModel.updateTextPosition(id, x, y)
                        },
                        onTextScaled = { id, size ->
                            viewModel.updateTextSize(id, size)
                        },
                        onTextRotated = { id, rotation ->
                            viewModel.updateTextRotation(id, rotation)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Text editing panel
            state.selectedTextId?.let { selectedId ->
                val selectedOverlay = state.textOverlays.find { it.id == selectedId }
                selectedOverlay?.let { overlay ->
                    TextEditorPanel(
                        textOverlay = overlay,
                        onUpdate = { viewModel.updateTextOverlay(it) },
                        onDelete = { viewModel.deleteTextOverlay(selectedId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCardEditor(
    imageUri: Uri?,
    bitmap: Bitmap?,
    textOverlays: List<TextOverlay>,
    selectedTextId: String?,
    onTextSelected: (String?) -> Unit,
    onTextMoved: (String, Float, Float) -> Unit,
    onTextScaled: (String, Float) -> Unit,
    onTextRotated: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background image - use bitmap if available, otherwise use URI
        bitmap?.let { bmp ->
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Text overlays
        textOverlays.forEach { overlay ->
            DraggableText(
                textOverlay = overlay,
                isSelected = overlay.id == selectedTextId,
                onClick = { onTextSelected(overlay.id) },
                onDrag = { x, y -> onTextMoved(overlay.id, x, y) },
                onScale = { size -> onTextScaled(overlay.id, size) },
                onRotate = { rotation -> onTextRotated(overlay.id, rotation) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DraggableText(
    textOverlay: TextOverlay,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onScale: (Float) -> Unit,
    onRotate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    var offsetX by remember { mutableFloatStateOf(textOverlay.x) }
    var offsetY by remember { mutableFloatStateOf(textOverlay.y) }
    var tempFontSize by remember { mutableFloatStateOf(textOverlay.fontSize.value) }
    var tempRotation by remember { mutableFloatStateOf(textOverlay.rotation) }

    LaunchedEffect(textOverlay.id, textOverlay.x, textOverlay.y) {
        offsetX = textOverlay.x
        offsetY = textOverlay.y
    }
    LaunchedEffect(textOverlay.id, textOverlay.fontSize.value) {
        tempFontSize = textOverlay.fontSize.value
    }
    LaunchedEffect(textOverlay.id, textOverlay.rotation) {
        tempRotation = textOverlay.rotation
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Box provides a larger touch target so both fingers can land within bounds,
        // enabling detectTransformGestures to receive two-pointer events for pinch/rotate.
        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .graphicsLayer { rotationZ = tempRotation }
                .defaultMinSize(minWidth = 120.dp, minHeight = 80.dp)
                .pointerInput(textOverlay.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onClick()
                        offsetX += pan.x / density
                        offsetY += pan.y / density
                        onDrag(offsetX, offsetY)
                        tempFontSize = (tempFontSize * zoom).coerceIn(12f, 200f)
                        onScale(tempFontSize)
                        tempRotation += rotation
                        onRotate(tempRotation)
                    }
                }
        ) {
            Text(
                text = textOverlay.text,
                modifier = Modifier.drawBehind {
                    if (isSelected) {
                        drawRect(color = Color.White.copy(alpha = 0.3f), size = size)
                    }
                },
                style = TextStyle(
                    color = textOverlay.color,
                    fontSize = tempFontSize.sp,
                    fontWeight = textOverlay.fontWeight
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextEditorPanel(
    textOverlay: TextOverlay,
    onUpdate: (TextOverlay) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(textOverlay.id) { mutableStateOf(textOverlay.text) }
    var fontSize by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.fontSize.value) }
    var rotation by remember(textOverlay.id) { mutableFloatStateOf(textOverlay.rotation) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Text input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onUpdate(textOverlay.copy(text = it))
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Font size slider
        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = fontSize,
            onValueChange = {
                fontSize = it
                onUpdate(textOverlay.copy(fontSize = it.sp))
            },
            valueRange = 12f..72f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rotation slider
        Text("Rotation: ${rotation.toInt()}°", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = rotation,
            onValueChange = {
                rotation = it
                onUpdate(textOverlay.copy(rotation = it))
            },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Color picker
        Text("Color:", style = MaterialTheme.typography.bodySmall)
        ColorPicker(
            selectedColor = textOverlay.color,
            onColorSelected = { onUpdate(textOverlay.copy(color = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Font weight options
        Text("Font Weight:", style = MaterialTheme.typography.bodySmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val weights = listOf(
                FontWeight.Light to "Light",
                FontWeight.Normal to "Normal",
                FontWeight.Medium to "Medium",
                FontWeight.Bold to "Bold",
                FontWeight.Black to "Black"
            )
            weights.forEach { (weight, label) ->
                FilterChip(
                    selected = textOverlay.fontWeight == weight,
                    onClick = { onUpdate(textOverlay.copy(fontWeight = weight)) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.White, Color.Black, Color.Red, Color.Blue, Color.Green,
        Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray,
        Color(0xFFFF5722), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF795548)  // Brown
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            val isSelected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .let { modifier ->
                        if (isSelected) {
                            modifier
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(2.dp)
                        } else {
                            modifier
                        }
                    }
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == Color.White || color == Color.Yellow || color == Color.Cyan) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private suspend fun saveAndShareImage(
    context: android.content.Context,
    state: com.odom.photocard.viewmodel.PhotoCardState,
    viewModel: PhotoCardViewModel
) {
    viewModel.setSavingState(true)
    
    try {
        withContext(Dispatchers.IO) {
            // Create bitmap with text overlays
            val finalBitmap = createFinalBitmap(state)
            
            // Save to cache file
            val cacheFile = File(context.cacheDir, "photocard_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cacheFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // Share the image
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Photo Card")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            viewModel.markSaveSuccess()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        viewModel.setSavingState(false)
    }
}

private fun createFinalBitmap(state: com.odom.photocard.viewmodel.PhotoCardState): Bitmap {
    val width = 1080
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw background image if available
    state.bitmap?.let { bgBitmap ->
        // Calculate scale to fit the bitmap to canvas while maintaining aspect ratio
        val scaleX = width.toFloat() / bgBitmap.width
        val scaleY = height.toFloat() / bgBitmap.height
        val scale = kotlin.math.max(scaleX, scaleY)
        
        val scaledWidth = bgBitmap.width * scale
        val scaledHeight = bgBitmap.height * scale
        
        // Center the scaled image
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        
        val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bgBitmap, null, destRect, null)
    } ?: run {
        // Fill with white background if no image
        canvas.drawColor(android.graphics.Color.WHITE)
    }
    
    // Draw text overlays
    val paint = Paint().apply {
        isAntiAlias = true
    }

    state.textOverlays.forEach { overlay ->
        paint.apply {
            color = overlay.color.toArgb()
            textSize = overlay.fontSize.value * 3f // Scale up for bitmap
            typeface = when (overlay.fontWeight) {
                FontWeight.Bold -> Typeface.DEFAULT_BOLD
                FontWeight.Light -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                else -> Typeface.DEFAULT
            }
        }

        val x = overlay.x * 3f
        val y = overlay.y * 3f + paint.textSize

        // Apply rotation if needed
        if (overlay.rotation != 0f) {
            canvas.save()
            // Rotate around the text position
            canvas.rotate(overlay.rotation, x, y - paint.textSize / 2)
            canvas.drawText(overlay.text, x, y, paint)
            canvas.restore()
        } else {
            canvas.drawText(overlay.text, x, y, paint)
        }
    }

    return bitmap
}
