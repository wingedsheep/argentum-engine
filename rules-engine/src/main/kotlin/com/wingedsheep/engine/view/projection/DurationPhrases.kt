package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * When an effect ends, phrased for a badge both players read: "your" becomes the controller's
 * name, and a "until the end of your next turn" effect already on its last turn says "until end of
 * turn". Null when the effect has no end.
 *
 * [expiresAfterTurn] is [ActiveFloatingEffect.expiresAfterTurn]: the cleanup that ends an
 * [Duration.EndOfYourNextTurn] effect is the first one on its controller's turn at or after it
 * (and the upkeep that ends a [Duration.EndOfYourNextUpkeep] one likewise).
 */
internal fun durationPhrase(
    state: GameState,
    duration: Duration,
    controllerId: EntityId,
    expiresAfterTurn: Int? = null,
): String? {
    if (duration == Duration.Permanent) return null
    if (duration == Duration.EndOfYourNextTurn && expiresAfterTurn != null &&
        state.activePlayerId == controllerId && state.turnNumber >= expiresAfterTurn
    ) return Duration.EndOfTurn.description
    if (duration == Duration.EndOfYourNextUpkeep && expiresAfterTurn != null &&
        state.isActiveTurnFor(controllerId) && state.turnNumber >= expiresAfterTurn
    ) return "until the end of this upkeep"
    val controller = state.getEntity(controllerId)?.get<PlayerComponent>()?.name ?: "its controller"
    return duration.description
        .replace("your ", "$controller's ")
        .replace("you control", "$controller controls")
}

internal fun durationPhrase(state: GameState, floating: ActiveFloatingEffect): String? {
    // "During its controller's next turn", already on that turn: it ends at this turn's cleanup.
    val floor = floating.expiresAfterTurn
    if (floating.duration == Duration.EndOfAffectedControllersNextTurn && floor != null &&
        state.turnNumber >= floor
    ) {
        val activeTeam = state.activePlayerId?.let { state.sharedTurnTeam(it) }.orEmpty()
        if (floating.effect.affectedEntities.any { state.projectedState.getController(it) in activeTeam }) {
            return Duration.EndOfTurn.description
        }
    }
    return durationPhrase(state, floating.duration, floating.controllerId, floating.expiresAfterTurn)
}

/**
 * The endings of effects combined into one badge: a single ending as itself, several as a list
 * ("until end of turn; indefinitely"), with [amounts] saying how much ends when if given. Null when
 * nothing ends.
 */
internal fun combinedEnding(ends: List<String?>, amounts: List<Int>? = null): String? {
    if (ends.all { it == null }) return null
    val byEnd = ends.indices.groupBy { ends[it] }
    if (byEnd.size == 1) return ends.first()
    return byEnd.entries.joinToString("; ") { (end, indices) ->
        val amount = amounts?.let { list -> "${indices.sumOf { list[it] }} " }.orEmpty()
        amount + (end ?: "indefinitely")
    }
}
