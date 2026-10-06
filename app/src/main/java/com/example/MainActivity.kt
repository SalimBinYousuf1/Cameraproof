package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AppTheme
import com.example.ui.navigation.Screen
import com.example.ui.screens.camera.CameraScreen
import com.example.ui.screens.camera.CameraViewModel
import com.example.ui.screens.gallery.GalleryViewModel
import com.example.ui.screens.gallery.PhotoViewerScreen
import com.example.ui.screens.gallery.ProjectGalleryScreen
import com.example.ui.screens.projects.ProjectListScreen
import com.example.ui.screens.projects.ProjectViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.SalimTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = SalimApplication.instance
            val themeSetting by app.settingsDataStore.appThemeFlow.collectAsState(initial = AppTheme.SYSTEM)

            val darkTheme = when (themeSetting) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            SalimTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SalimAppNavHost(app = app)
                }
            }
        }
    }
}

@Composable
fun SalimAppNavHost(app: SalimApplication) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Projects.route
    ) {
        // Projects List
        composable(Screen.Projects.route) {
            val projectViewModel: ProjectViewModel = viewModel(
                factory = ProjectViewModel.Factory(app)
            )
            ProjectListScreen(
                viewModel = projectViewModel,
                onProjectClicked = { projectId ->
                    navController.navigate(Screen.Gallery.createRoute(projectId))
                },
                onCameraClicked = { projectId ->
                    navController.navigate(Screen.Camera.createRoute(projectId))
                },
                onSettingsClicked = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Camera Screen
        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val projectIdStr = backStackEntry.arguments?.getString("projectId")
            val projectId = projectIdStr?.toLongOrNull()

            val cameraViewModel: CameraViewModel = viewModel(
                factory = CameraViewModel.Factory(app, projectId)
            )

            CameraScreen(
                viewModel = cameraViewModel,
                onNavigateToGallery = { pid ->
                    navController.navigate(Screen.Gallery.createRoute(pid)) {
                        popUpTo(Screen.Projects.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Project Gallery Screen
        composable(
            route = Screen.Gallery.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val galleryViewModel: GalleryViewModel = viewModel(
                factory = GalleryViewModel.Factory(app, projectId)
            )

            ProjectGalleryScreen(
                viewModel = galleryViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = { pid ->
                    navController.navigate(Screen.Camera.createRoute(pid))
                },
                onPhotoClicked = { photoId ->
                    navController.navigate(Screen.Viewer.createRoute(photoId, projectId))
                }
            )
        }

        // Fullscreen Photo Viewer Screen
        composable(
            route = Screen.Viewer.route,
            arguments = listOf(
                navArgument("photoId") { type = NavType.LongType },
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val galleryViewModel: GalleryViewModel = viewModel(
                factory = GalleryViewModel.Factory(app, projectId)
            )

            PhotoViewerScreen(
                viewModel = galleryViewModel,
                initialPhotoId = photoId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app)
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
