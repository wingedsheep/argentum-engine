package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import java.security.MessageDigest

/**
 * A short, cheap digest of an observable game position.
 *
 * A recorded replay is an input stream that only reproduces the original game while the engine that
 * folds it stays behaviourally identical. When it doesn't — a card's implementation changed, a rules
 * fix landed — the re-simulation silently drifts: the actions keep applying, but the board they
 * produce is no longer the board that was played. Truncation (an action that outright fails) is the
 * loud half of that failure; drift is the quiet half, and the quiet half is worse because the viewer
 * happily renders a game that never happened.
 *
 * So the recorder stamps a fingerprint every [ReplayRecordingPolicy.CHECKPOINT_EVERY_ACTIONS]
 * actions while the game is live, and reconstruction re-computes it at the same points. A mismatch
 * pins divergence to a window of actions and downgrades the replay's
 * [ReplayFidelity] instead of pretending nothing happened.
 *
 * The fields below are the ones that move when the engine disagrees with its past self: the entity
 * counter (every minted id is drawn from it, so any extra/missing object shifts it), the game clock,
 * turn/phase/priority, zone sizes, and life totals. Deliberately *not* the whole state — this runs
 * inside the live game loop, and a full serialize-and-hash per checkpoint would be real work for a
 * marginally sharper signal.
 */
object ReplayFingerprint {

    /** A 16-hex-char digest of [state]'s observable position. */
    fun of(state: GameState): String {
        val sb = StringBuilder(256)
        sb.append(state.turnNumber).append('|')
            .append(state.phase).append('|')
            .append(state.step).append('|')
            .append(state.activePlayerId?.value ?: "-").append('|')
            .append(state.priorityPlayerId?.value ?: "-").append('|')
            .append(state.nextEntityId).append('|')
            .append(state.timestamp).append('|')
            .append(state.stack.size).append('|')
            .append(state.gameOver).append('|')
            .append(state.winnerId?.value ?: "-").append('|')
            .append(state.pendingDecision?.let { it::class.simpleName } ?: "-").append('|')

        // Zone sizes, in a stable order (map iteration order is not guaranteed across runs).
        state.zones.entries
            .map { (key, ids) -> "${key.ownerId.value}:${key.zoneType}=${ids.size}" }
            .sorted()
            .forEach { sb.append(it).append(',') }
        sb.append('|')

        // Life totals in turn order — the single most player-visible number a divergence moves.
        for (playerId in state.turnOrder) {
            // The resolver, not the raw component: a 2HG team's life lives on one member (CR 810.9a).
            val life = if (state.getEntity(playerId)?.get<LifeTotalComponent>() != null) state.lifeTotal(playerId) else 0
            sb.append(playerId.value).append('=').append(life).append(',')
        }

        return digest(sb.toString())
    }

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(16) {
            for (i in 0 until 8) append("%02x".format(bytes[i]))
        }
    }
}
