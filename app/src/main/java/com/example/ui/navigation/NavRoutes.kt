package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Projects : Screen("projects")
    data object Camera : Screen("camera?projectId={projectId}") {
        fun createRoute(projectId: Long?): String =
            if (projectId != null) "camera?projectId=$projectId" else "camera"
    }
    data object Gallery : Screen("gallery/{projectId}") {
        fun createRoute(projectId: Long): String = "gallery/$projectId"
    }
    data object Viewer : Screen("viewer/{photoId}/{projectId}") {
        fun createRoute(photoId: Long, projectId: Long): String = "viewer/$photoId/$projectId"
    }
    data object Settings : Screen("settings")
}
