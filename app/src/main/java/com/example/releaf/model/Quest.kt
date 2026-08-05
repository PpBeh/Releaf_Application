package com.example.releaf.model

enum class QuestStatus {
    IN_PROGRESS,
    CLAIMABLE,
    CLAIMED
}

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val rewardLabel: String,
    val rewardCount: Int,
    val progressCurrent: Int,
    val progressTarget: Int,
    val status: QuestStatus
) {
    companion object {
        // TODO: replace with a real data source later
        fun sampleList(): List<Quest> = emptyList()
    }
}