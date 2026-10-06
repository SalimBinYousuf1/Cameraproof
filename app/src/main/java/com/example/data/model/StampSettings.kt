package com.example.data.model

enum class StampPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT
}

enum class StampTextSize {
    SMALL,
    MEDIUM,
    LARGE
}

data class StampSettings(
    val showDateTime: Boolean = true,
    val use24Hour: Boolean = false,
    val showCoordinates: Boolean = true,
    val showAddress: Boolean = true,
    val showProjectName: Boolean = true,
    val showNote: Boolean = true,
    val position: StampPosition = StampPosition.BOTTOM_LEFT,
    val textSize: StampTextSize = StampTextSize.MEDIUM
)
