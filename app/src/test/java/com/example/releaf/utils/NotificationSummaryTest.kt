package com.example.releaf.utils

import org.junit.Test
import org.junit.Assert.*

class NotificationSummaryTest {

    @Test
    fun firstLoad_withExistingUnread_doesNotBuzz() {
        val decision = NotificationSummary.decide(
            state = NotificationSummary.State(),
            phoneAllowed = true,
            allIds = setOf("a", "b"),
            unreadLikeIds = setOf("a", "b")
        )

        assertEquals(NotificationSummary.Action.None, decision.action)
        assertFalse(decision.state.isFirstLoad)
        assertEquals(setOf("a", "b"), decision.state.knownIds)
        assertEquals(0, decision.state.lastPostedUnreadCount)
    }

    @Test
    fun firstLoad_withNothing_cancelsStaleShadeEntry() {
        val decision = NotificationSummary.decide(
            state = NotificationSummary.State(),
            phoneAllowed = true,
            allIds = emptySet(),
            unreadLikeIds = emptySet()
        )

        assertEquals(NotificationSummary.Action.Cancel, decision.action)
    }

    @Test
    fun newLike_buzzesOnceWithCount() {
        val baseline = NotificationSummary.decide(
            state = NotificationSummary.State(),
            phoneAllowed = true,
            allIds = emptySet(),
            unreadLikeIds = emptySet()
        ).state

        val decision = NotificationSummary.decide(
            state = baseline,
            phoneAllowed = true,
            allIds = setOf("a"),
            unreadLikeIds = setOf("a")
        )

        assertEquals(NotificationSummary.Action.Post(unreadCount = 1, alert = true), decision.action)
        assertEquals(1, decision.state.lastPostedUnreadCount)
    }

    @Test
    fun unchangedPoll_doesNothing_noRebuzz() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state
        state = NotificationSummary.decide(state, true, setOf("a"), setOf("a")).state

        // Same list again (the 10s poll): must stay silent.
        val decision = NotificationSummary.decide(state, true, setOf("a"), setOf("a"))

        assertEquals(NotificationSummary.Action.None, decision.action)
        assertEquals(1, decision.state.lastPostedUnreadCount)
    }

    @Test
    fun secondLike_updatesCountWithBuzz() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state
        state = NotificationSummary.decide(state, true, setOf("a"), setOf("a")).state

        val decision = NotificationSummary.decide(state, true, setOf("a", "b"), setOf("a", "b"))

        assertEquals(NotificationSummary.Action.Post(unreadCount = 2, alert = true), decision.action)
    }

    @Test
    fun readInApp_refreshesTextSilently() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state
        state = NotificationSummary.decide(state, true, setOf("a", "b"), setOf("a", "b")).state

        // User read "a" in-app (no new arrivals, count dropped 2 -> 1).
        val decision = NotificationSummary.decide(state, true, setOf("a", "b"), setOf("b"))

        assertEquals(NotificationSummary.Action.Post(unreadCount = 1, alert = false), decision.action)
    }

    @Test
    fun allRead_cancelsSummary() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state
        state = NotificationSummary.decide(state, true, setOf("a"), setOf("a")).state

        val decision = NotificationSummary.decide(state, true, setOf("a"), emptySet())

        assertEquals(NotificationSummary.Action.Cancel, decision.action)
        assertEquals(0, decision.state.lastPostedUnreadCount)
    }

    @Test
    fun toggleOff_absorbsRowsSilentlyAndCancelsLiveSummary() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state
        state = NotificationSummary.decide(state, true, setOf("a"), setOf("a")).state

        // New like "b" arrives while the toggle is OFF: no buzz, but "b" is
        // remembered so re-enabling later can never burst it.
        val offDecision = NotificationSummary.decide(state, false, setOf("a", "b"), setOf("a", "b"))
        assertEquals(NotificationSummary.Action.Cancel, offDecision.action)

        // Toggle back ON with nothing new: must stay silent.
        val onDecision = NotificationSummary.decide(offDecision.state, true, setOf("a", "b"), setOf("a", "b"))
        assertEquals(NotificationSummary.Action.None, onDecision.action)
    }

    @Test
    fun toggleOff_withNothingLive_staysSilent() {
        val state = NotificationSummary.State(
            isFirstLoad = false,
            knownIds = setOf("a"),
            lastPostedUnreadCount = 0
        )

        val decision = NotificationSummary.decide(state, false, setOf("a"), setOf("a"))

        assertEquals(NotificationSummary.Action.None, decision.action)
    }

    @Test
    fun likeUnlikeRelike_onlyBuzzesOnFreshArrivals() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state

        // Like -> buzz.
        var decision = NotificationSummary.decide(state, true, setOf("v1"), setOf("v1"))
        assertEquals(NotificationSummary.Action.Post(1, true), decision.action)
        state = decision.state

        // Unlike (vote row removed server-side, notification row stays read or is
        // deleted -> unread set shrinks, no fresh ids): silent refresh, no buzz.
        decision = NotificationSummary.decide(state, true, emptySet(), emptySet())
        assertEquals(NotificationSummary.Action.Cancel, decision.action)
        state = decision.state

        // Relike -> brand-new row id -> buzz once with updated count.
        decision = NotificationSummary.decide(state, true, setOf("v2"), setOf("v2"))
        assertEquals(NotificationSummary.Action.Post(1, true), decision.action)
    }

    @Test
    fun arrivalsWhileOff_thenReopen_staysSilent() {
        var state = NotificationSummary.State()
        state = NotificationSummary.decide(state, true, emptySet(), emptySet()).state

        // Toggle off with nothing live: silent.
        var decision = NotificationSummary.decide(state, false, emptySet(), emptySet())
        assertEquals(NotificationSummary.Action.None, decision.action)
        state = decision.state

        // Two likes arrive across polls while off: never a buzz (the second
        // Cancel targets an already-empty shade slot — a harmless no-op).
        decision = NotificationSummary.decide(state, false, setOf("a"), setOf("a"))
        assertEquals(NotificationSummary.Action.None, decision.action)
        state = decision.state

        decision = NotificationSummary.decide(state, false, setOf("a", "b"), setOf("a", "b"))
        assertEquals(NotificationSummary.Action.Cancel, decision.action)
        state = decision.state

        // Reopen: everything was already absorbed -> silent.
        decision = NotificationSummary.decide(state, true, setOf("a", "b"), setOf("a", "b"))
        assertEquals(NotificationSummary.Action.None, decision.action)
        state = decision.state

        // A third like after reopen -> buzzes once with the fresh count.
        decision = NotificationSummary.decide(state, true, setOf("a", "b", "c"), setOf("a", "b", "c"))
        assertEquals(NotificationSummary.Action.Post(unreadCount = 3, alert = true), decision.action)
    }
}
