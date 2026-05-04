package com.odom.photocard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.odom.photocard.ui.ImageSourceType
import com.odom.photocard.viewmodel.PhotoCardViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for selecting image source (Camera or Gallery)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceScreen(
    viewModel: PhotoCardViewModel,
    onImageSelected: (ImageSourceType) -> Unit,
    onNavigateToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionToRequest by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImage(it, context)
            onImageSelected(ImageSourceType.CAMERA_GALLERY)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let {
                viewModel.setImage(it, context)
                onImageSelected(ImageSourceType.CAMERA_GALLERY)
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (permissionToRequest) {
                Manifest.permission.CAMERA -> {
                    // Create temporary file for camera photo
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val photoFile = File.createTempFile(
                        "JPEG_${timeStamp}_",
                        ".jpg",
                        context.cacheDir
                    )
                    photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        photoFile
                    )
                    photoUri?.let { cameraLauncher.launch(it) }
                }
                Manifest.permission.READ_MEDIA_IMAGES -> {
                    galleryLauncher.launch("image/*")
                }
            }
        }
        permissionToRequest = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Photo Card") },
                actions = {
                    IconButton(onClick = onNavigateToGallery) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = "My Cards",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Select an image to create your photo card",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Camera option
            SourceOptionCard(
                icon = Icons.Default.AddAPhoto,
                title = "Take Photo",
                description = "Capture a new photo with your camera",
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            // Create temporary file for camera photo
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val photoFile = File.createTempFile(
                                "JPEG_${timeStamp}_",
                                ".jpg",
                                context.cacheDir
                            )
                            photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                photoFile
                            )
                            photoUri?.let { cameraLauncher.launch(it) }
                        }
                        else -> {
                            permissionToRequest = Manifest.permission.CAMERA
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gallery option
            SourceOptionCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Choose from Gallery",
                description = "Select an existing photo from your device",
                onClick = {
                    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    
                    when {
                        ContextCompat.checkSelfPermission(
                            context, permission
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            galleryLauncher.launch("image/*")
                        }
                        else -> {
                            permissionToRequest = permission
                            permissionLauncher.launch(permission)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sample Images option
            SourceOptionCard(
                icon = Icons.Default.Shuffle,
                title = "Sample Images",
                description = "Use a random online image",
                onClick = {
                    val randomUrl = "${PhotoCardViewModel.SAMPLE_IMAGE_URL}?t=${System.currentTimeMillis()}"
                    viewModel.loadSampleImage(randomUrl)
                    viewModel.addSampleQuote()
                    onImageSelected(ImageSourceType.SAMPLE_IMAGE)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Solid Color option
            SourceOptionCard(
                icon = Icons.Default.ColorLens,
                title = "Solid Color",
                description = "Create a photo card with a colored background",
                onClick = {
                    showColorPicker = true
                }
            )

            // Color Picker Dialog
            if (showColorPicker) {
                ColorPickerDialog(
                    onColorSelected = { colorInt ->
                        viewModel.createSolidColorBitmap(colorInt)
                        viewModel.addSampleQuote()
                        showColorPicker = false
                        onImageSelected(ImageSourceType.SOLID_COLOR)
                    },
                    onDismiss = {
                        showColorPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SourceOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        android.graphics.Color.parseColor("#FF6B6B"), // Red
        android.graphics.Color.parseColor("#FF8E53"), // Orange
        android.graphics.Color.parseColor("#F7DC6F"), // Yellow
        android.graphics.Color.parseColor("#96CEB4"), // Green
        android.graphics.Color.parseColor("#4ECDC4"), // Teal
        android.graphics.Color.parseColor("#45B7D1"), // Blue
        android.graphics.Color.parseColor("#85C1E9"), // Sky
        android.graphics.Color.parseColor("#BB8FCE"), // Lavender
        android.graphics.Color.parseColor("#DDA0DD"), // Plum
        android.graphics.Color.parseColor("#F8C8DC"), // Pink
        android.graphics.Color.parseColor("#2C3E50"), // Dark Blue
        android.graphics.Color.parseColor("#34495E"), // Gray Blue
        android.graphics.Color.parseColor("#1ABC9C"), // Emerald
        android.graphics.Color.parseColor("#16A085"), // Green Sea
        android.graphics.Color.parseColor("#E74C3C"), // Alizarin
        android.graphics.Color.parseColor("#C0392B"), // Pomegranate
        android.graphics.Color.parseColor("#95A5A6"), // Concrete
        android.graphics.Color.parseColor("#7F8C8D"), // Asbestos
        android.graphics.Color.parseColor("#2ECC71"), // Nephritis
        android.graphics.Color.parseColor("#27AE60")  // Sea Green
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Choose Background Color",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colors.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { colorInt ->
                                Surface(
                                    onClick = { onColorSelected(colorInt) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = androidx.compose.ui.graphics.Color(colorInt),
                                    modifier = Modifier
                                        .size(56.dp)
                                ) {}
                            }
                        }
                    }
                }

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
