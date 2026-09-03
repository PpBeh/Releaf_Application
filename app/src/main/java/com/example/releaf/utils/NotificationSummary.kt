package com.example.releaf.utils

/**
 * Pure decision logic for the aggregated phone-notification summary.
 *
 * Kept free of Android APIs so it can be unit-tested on the JVM. The single
 * summary ("You have N notifications!") updates in place; [Action.Post] with
 * [Action.Post.alert] = true buzzes once for genuinely new arrivals, false
 * refreshes the text silently.
 */
object NotificationSummary {

    data class State(
        val isFirstLoad: Boolean = true,
        val knownIds: Set<String> = emptySet(),
        val lastPostedUnreadCount: Int = 0
    )

    sealed interface Action {
        data object None : Action
        data class Post(val unreadCount: Int, val alert: Boolean) : Action
        data object Cancel : Action
    }

    data class Decision(val state: State, val action: Action)

    fun decide(
        state: State,
        phoneAllowed: Boolean,
        allIds: Set<String>,
        unreadLikeIds: Set<String>
    ): Decision {
        if (state.isFirstLoad) {
            // Baseline on cold start: never buzz for rows that already exist.
            // Cancel in case a stale shade entry survived from an older session.
            val next = state.copy(
                isFirstLoad = false,
                knownIds = allIds,
                lastPostedUnreadCount = 0
            )
            return Decision(next, if (unreadLikeIds.isEmpty()) Action.Cancel else Action.None)
        }

        val freshIds = unreadLikeIds - state.knownIds
        // Remember everything currently visible, buzzing or not, so re-enabling
        // the setting later can never burst old rows.
        val nextKnown = state.knownIds + unreadLikeIds
        val count = unreadLikeIds.size

        if (!phoneAllowed) {
            // Absorb silently: keep tracking the count so reopening the setting
            // finds nothing new to buzz about. Cancel once on the way down only.
            val next = state.copy(knownIds = nextKnown, lastPostedUnreadCount = count)
            return Decision(
                next,
                if (state.lastPostedUnreadCount > 0) Action.Cancel else Action.None
            )
        }

        if (unreadLikeIds.isEmpty()) {
            val next = state.copy(knownIds = nextKnown, lastPostedUnreadCount = 0)
            return Decision(
                next,
                if (state.lastPostedUnreadCount > 0) Action.Cancel else Action.None
            )
        }

        val next = state.copy(knownIds = nextKnown, lastPostedUnreadCount = count)
        return when {
            freshIds.isNotEmpty() -> Decision(next, Action.Post(count, alert = true))
            count != state.lastPostedUnreadCount -> Decision(next, Action.Post(count, alert = false))
            else -> Decision(next, Action.None)
        }
    }
}
