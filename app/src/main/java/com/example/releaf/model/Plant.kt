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
    val nickname: String = "",
    val personality: String = "",
    val mood: String = "",
    val quote: String = "",
    val description: String = "",
    val careTip: String = "",
    val drawableRes: Int
)

fun isPlantedState(state: String?): Boolean {
    return state in setOf("PLANTED", "GROWING", "FULLY_GROWN")
}

object SeedData {
    val seedList = listOf(
        SeedInfo(
            slotIndex = 1,
            targetPoints = 50,
            name = "Sunflower",
            nickname = "Sunny",
            personality = "Cheerful • Energetic • Sun-Chaser",
            mood = "Feeling bright & sun-kissed! ☀️",
            quote = "\"Always look towards the light, my friend!\"",
            description = "A vibrant yellow flower that turns to face the morning sun. Brings optimism and energy to your garden.",
            careTip = "Water daily and give plenty of direct sunlight.",
            drawableRes = R.drawable.ic_plant_1
        ),
        SeedInfo(
            slotIndex = 2,
            targetPoints = 150,
            name = "Green Cactus",
            nickname = "Cacti",
            personality = "Resilient • Cool • Vibrant",
            mood = "Soaking up the warm sun rays! 🌵",
            quote = "\"I stay green through any storm!\"",
            description = "A lush green cactus that stands proud and thrives in bright sunlight.",
            careTip = "Enjoys warm sunlight and light watering.",
            drawableRes = R.drawable.ic_plant_2
        ),
        SeedInfo(
            slotIndex = 3,
            targetPoints = 300,
            name = "Fuzzy Bush",
            nickname = "Fuzzy",
            personality = "Soft • Cozy • Friendly",
            mood = "Super soft and ready for a gentle pat! 🌿",
            quote = "\"Stay cozy and grow soft!\"",
            description = "An adorable plush green bush with soft rounded leaves that feels cuddly and warm.",
            careTip = "Enjoys gentle watering and cozy indirect light.",
            drawableRes = R.drawable.ic_plant_3
        ),
        SeedInfo(
            slotIndex = 4,
            targetPoints = 500,
            name = "Tall Pine",
            nickname = "Piney",
            personality = "Strong • Majestic • Everlasting",
            mood = "Reaching high for the mountain sky! 🌲",
            quote = "\"Stand tall and reach for the stars!\"",
            description = "A strong evergreen pine tree with fragrant needles that grows tall and proud.",
            careTip = "Requires good drainage and fresh open air.",
            drawableRes = R.drawable.ic_plant_4
        ),
        SeedInfo(
            slotIndex = 5,
            targetPoints = 800,
            name = "Lush Monstera",
            nickname = "Monty",
            personality = "Playful • Trendy • Jungle Enthusiast",
            mood = "Unfurling a brand new leaf today! 🌿",
            quote = "\"More split leaves, more good vibes!\"",
            description = "A tropical icon known for its wide split green leaves. Loves to climb high and fill the space with tropical joy.",
            careTip = "Keep in indirect sunlight and wipe leaves regularly.",
            drawableRes = R.drawable.ic_plant_5
        ),
        SeedInfo(
            slotIndex = 6,
            targetPoints = 1200,
            name = "Red Mushroom",
            nickname = "Shroomy",
            personality = "Mystical • Whimsical • Spore-Spreader",
            mood = "Glowing magically under the forest shade! 🍄",
            quote = "\"There's magic under every cap!\"",
            description = "A charming red mushroom with white spots that glows with magical forest charm. Adds a touch of fairytale mystery to your garden.",
            careTip = "Loves shade, organic soil, and high humidity.",
            drawableRes = R.drawable.ic_plant_6
        )
    )

    fun getSeedForSlot(slotIndex: Int): SeedInfo {
        return seedList.find { it.slotIndex == slotIndex } ?: seedList[0]
    }
}
