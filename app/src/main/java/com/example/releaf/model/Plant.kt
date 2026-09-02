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
            name = "Evergreen Bonsai",
            nickname = "Zen Master",
            personality = "Calm • Wise • Patient",
            mood = "Meditating peacefully in the breeze... 🧘",
            quote = "\"Growth is not a race, it is an art form.\"",
            description = "A delicate miniature tree cultivated with extreme patience and care. Radiates tranquil energy.",
            careTip = "Prune gently and water with mindfulness.",
            drawableRes = R.drawable.ic_plant_2
        ),
        SeedInfo(
            slotIndex = 3,
            targetPoints = 300,
            name = "Prickly Cactus",
            nickname = "Spike",
            personality = "Sassy • Independent • Soft at Heart",
            mood = "Don't touch unless you've brought water! 🌵",
            quote = "\"I'm tough on the outside, but I blossom big!\"",
            description = "A resilient desert survivor that thrives in tough conditions. Underneath its spikes lies a gentle bloom.",
            careTip = "Thrives with minimal watering and plenty of warmth.",
            drawableRes = R.drawable.ic_plant_3
        ),
        SeedInfo(
            slotIndex = 4,
            targetPoints = 500,
            name = "Blooming Rose",
            nickname = "Rosie",
            personality = "Romantic • Passionate • Elegant",
            mood = "In full bloom and smelling divine! 🌹",
            quote = "\"Elegance is an attitude, and so is blooming!\"",
            description = "A classic velvet rose blooming with rich fragrance and deep colors. The undisputed queen of the garden.",
            careTip = "Enjoys organic fertilizer and gentle morning misting.",
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
            name = "Golden Oak",
            nickname = "Aurelius",
            personality = "Majestic • Guardian • Inspiring",
            mood = "Standing tall and glowing with golden sap! 🌳",
            quote = "\"Mighty oaks from tiny golden acorns grow.\"",
            description = "A majestic golden seedling of an ancient oak tree. Its golden leaves shimmer with royal elegance.",
            careTip = "Needs a sturdy pot, plenty of soil nutrients, and room to grow.",
            drawableRes = R.drawable.ic_plant_6
        )
    )

    fun getSeedForSlot(slotIndex: Int): SeedInfo {
        return seedList.find { it.slotIndex == slotIndex } ?: seedList[0]
    }
}
