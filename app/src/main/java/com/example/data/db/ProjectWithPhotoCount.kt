package com.example.data.db

import androidx.room.Embedded

data class ProjectWithPhotoCount(
    @Embedded
    val project: ProjectEntity,
    val photoCount: Int = 0,
    val latestPhotoPath: String? = null
)
