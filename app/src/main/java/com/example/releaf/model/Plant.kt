package com.example.releaf.model

import com.example.releaf.R

enum class PlantSlotState {
    LOCKED,
    EMPTY_POT,
    GROWING,
    FULLY_GROWN
}

data class PlantSlot(
    val id: String,
    val state: PlantSlotState,
    val plantDrawableRes: Int? = null
)

data class GardenProgress(
    val currentExp: Int,
    val expTarget: Int,
    val growUsesLeft: Int,
    val growUsesMax: Int,
    val fertilizeUsesLeft: Int,
    val fertilizeUsesMax: Int
)

data class SeedInfo(
    val slotIndex: Int,
    val targetPoints: Int,
    val name: String,
    val description: String,
    val drawableRes: Int
)

object SeedData {
    val seedList = listOf(
        SeedInfo(1, 50, "Sunflower", "A bright yellow flower that turns to face the sun.", R.drawable.ic_plant_1),
        SeedInfo(2, 150, "Evergreen Bonsai", "A delicate miniature tree cultivated with care.", R.drawable.ic_plant_2),
        SeedInfo(3, 300, "Prickly Cactus", "A resilient desert plant that thrives anywhere.", R.drawable.ic_plant_3),
        SeedInfo(4, 500, "Blooming Rose", "A classic red rose blooming with vibrant colors.", R.drawable.ic_plant_4),
        SeedInfo(5, 800, "Lush Monstera", "A tropical plant known for its split green leaves.", R.drawable.ic_plant_5),
        SeedInfo(6, 1200, "Golden Oak", "A majestic golden seedling of a giant oak tree.", R.drawable.ic_plant_6)
    )

    fun getSeedForSlot(slotIndex: Int): SeedInfo {
        return seedList.find { it.slotIndex == slotIndex } ?: seedList[0]
    }
}
