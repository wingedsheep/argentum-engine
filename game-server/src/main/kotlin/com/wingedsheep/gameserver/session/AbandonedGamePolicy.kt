package com.wingedsheep.gameserver.session

import java.time.Duration
import java.time.Instant

/**
 * When a game nobody is playing gets ended by [ZombieSessionSweeper]. A game normally ends on its own
 * — a result, a concession, the disconnect timer forfeiting a seat, or [GameStallGuard] calling a
 * draw — but none of those fire for a game whose humans walked away without the timer applying (or
 * whose AIs are waiting on something that will never come), so such a game sat in memory forever,
 * held its AI controllers, and showed up on the admin Live overview as "running for 14h".
 *
 * Two rules, both requiring that no human seat has an open socket (a connected human is always
 * someone playing, however slowly):
 *
 * - **Idle:** no action applied for [IDLE_LIMIT]. Covers every game, including the intentionally
 *   AI-only ones (dev LLM tournaments), which act constantly while alive.
 * - **Unattended:** the game has human seats, and none of them has been connected for
 *   [UNATTENDED_LIMIT] — the AIs of a table the human left can keep acting indefinitely, which the
 *   idle rule can't see. Games with no human seat at all are exempt: being AI-only is their point.
 *
 * Both limits sit far beyond the in-game disconnect timer and any tournament "add time", so neither
 * rule should ever end a game a player intends to come back to.
 */
object AbandonedGamePolicy {
    val IDLE_LIMIT: Duration = Duration.ofMinutes(30)
    val UNATTENDED_LIMIT: Duration = Duration.ofMinutes(60)

    /** What the policy reads off a game; see [of]. */
    data class Facts(
        val hasHumanSeat: Boolean,
        val hasConnectedHuman: Boolean,
        /** Last action, else the game's start, else the session's creation. */
        val lastActivityAt: Instant,
        val unattendedSince: Instant?,
    )

    fun of(session: GameSession): Facts {
        val seats = session.adminSnapshot().seats
        return Facts(
            hasHumanSeat = seats.any { !it.isAi },
            hasConnectedHuman = seats.any { !it.isAi && it.connected },
            lastActivityAt = session.lastActionAt ?: session.replayStartedAt ?: session.createdAt,
            unattendedSince = session.unattendedSince,
        )
    }

    /** When this game will be ended if nothing changes, or null while a human is connected. */
    fun endsAt(facts: Facts): Instant? {
        if (facts.hasConnectedHuman) return null
        val idleEnd = facts.lastActivityAt + IDLE_LIMIT
        val unattendedEnd = facts.unattendedSince?.takeIf { facts.hasHumanSeat }?.plus(UNATTENDED_LIMIT)
        return if (unattendedEnd != null && unattendedEnd < idleEnd) unattendedEnd else idleEnd
    }

    /** The player-facing reason to end this game now, or null to leave it running. */
    fun verdict(facts: Facts, now: Instant): String? {
        if (facts.hasConnectedHuman) return null
        val endsAt = endsAt(facts) ?: return null
        if (now < endsAt) return null
        return if (facts.lastActivityAt + IDLE_LIMIT <= now) {
            "The game was ended as a draw after ${IDLE_LIMIT.toMinutes()} minutes without a move or a connected player."
        } else {
            "The game was ended as a draw after ${UNATTENDED_LIMIT.toMinutes()} minutes without a connected player."
        }
    }
}
