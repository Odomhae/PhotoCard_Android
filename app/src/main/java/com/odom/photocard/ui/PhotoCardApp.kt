package com.odom.photocard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.odom.photocard.ui.screens.EditScreen
import com.odom.photocard.ui.screens.ImageEditScreen
import com.odom.photocard.ui.screens.ImageSourceScreen
import com.odom.photocard.viewmodel.PhotoCardViewModel

/**
 * Navigation routes for the Photo Card app
 */
sealed class Screen(val route: String) {
    data object ImageSource : Screen("image_source")
    data object ImageEdit : Screen("image_edit")
    data object Edit : Screen("edit")
}

/**
 * Image source type to determine navigation flow
 */
enum class ImageSourceType {
    CAMERA_GALLERY,  // Needs image editing
    SAMPLE_IMAGE,    // Skip editing, go to edit
    SOLID_COLOR      // Skip editing, go to edit
}

/**
 * Main app navigation host
 */
@Composable
fun PhotoCardApp(
    viewModel: PhotoCardViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.ImageSource.route
    ) {
        composable(Screen.ImageSource.route) {
            ImageSourceScreen(
                viewModel = viewModel,
                onImageSelected = { sourceType ->
                    when (sourceType) {
                        ImageSourceType.CAMERA_GALLERY -> {
                            navController.navigate(Screen.ImageEdit.route)
                        }
                        ImageSourceType.SAMPLE_IMAGE,
                        ImageSourceType.SOLID_COLOR -> {
                            // Skip image editing for samples and colors
                            navController.navigate(Screen.Edit.route)
                        }
                    }
                }
            )
        }

        composable(Screen.ImageEdit.route) {
            ImageEditScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditComplete = {
                    navController.navigate(Screen.Edit.route)
                }
            )
        }

        composable(Screen.Edit.route) {
            EditScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.clearState()
                    navController.popBackStack(Screen.ImageSource.route, false)
                }
            )
        }
    }
}
