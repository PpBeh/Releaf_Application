package com.example.releaf.model

enum class PlantSlotState {
    LOCKED,
    EMPTY_POT,
    GROWING,
    FULLY_GROWN
}

data class PlantSlot(
    val id: String,
    val state: PlantSlotState,
    val plantDrawableRes: Int? = null // TODO: point at the grown-plant illustration once assets are added
)

data class GardenProgress(
    val currentExp: Int,
    val expTarget: Int,
    val growUsesLeft: Int,
    val growUsesMax: Int,
    val fertilizeUsesLeft: Int,
    val fertilizeUsesMax: Int
)