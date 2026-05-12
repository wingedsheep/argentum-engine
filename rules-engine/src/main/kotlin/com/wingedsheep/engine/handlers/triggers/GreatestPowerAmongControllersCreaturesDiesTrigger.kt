package com.wingedsheep.engine.handlers.triggers

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GreatestPowerOpponentCreatureDiedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone

/**
 * Emits [GreatestPowerOpponentCreatureDiedEvent] for each observing player whenever a
 * creature dies that had the strictly greatest power among ALL creatures the same player
 * controlled at that instant (using [ZoneChangeEvent.lastKnownPower] for the dying
 * creature and projected P/T for the surviving creatures still on the battlefield).
 *
 * Ties are treated as "not strictly greatest": if two creatures share the highest power,
 * neither fires the trigger. (Magic rules do not define a canonical tie-breaker for
 * "creature with the greatest power," so ties are conservative-false here.)
 *
 * A creature that is the sole creature its controller controls does NOT satisfy the
 * condition: the comparison set must contain at least one other creature so that
 * "greatest among" is a meaningful predicate rather than a vacuous truth.
 *
 * Structure mirrors the canonical handler shape: event subscription → predicate → emission.
 */
class GreatestPowerAmongControllersCreaturesDiesTrigger {

    // event subscription: ZoneChangeEvent deaths (battlefield → graveyard)
    fun handle(state: GameState, events: List<GameEvent>): List<GreatestPowerOpponentCreatureDiedEvent> {
        val result = mutableListOf<GreatestPowerOpponentCreatureDiedEvent>()
        val projected = state.projectedState

        for (event in events) {
            if (event !is ZoneChangeEvent) continue
            if (event.fromZone != Zone.BATTLEFIELD || event.toZone != Zone.GRAVEYARD) continue

            // predicate: last-known power must be present (non-creature deaths carry null)
            val dyingPower = event.lastKnownPower ?: continue
            val controller = event.ownerId

            // Surviving creatures the same player controls at the instant of death
            // (the dying creature has already left the battlefield in the post-SBA state)
            val otherPowers = state.getBattlefield()
                .filter { id -> projected.getController(id) == controller }
                .mapNotNull { id -> projected.getPower(id) }

            // Require at least one other creature: a singleton is vacuously greatest
            // but the trigger semantics demand a real comparison set.
            if (otherPowers.isEmpty()) continue

            // Strictly greatest: dying power must exceed every surviving creature's power
            if (!otherPowers.all { it < dyingPower }) continue

            // emission: one event per player who considers the controller an opponent
            for (observerId in state.turnOrder) {
                if (observerId == controller) continue
                result.add(
                    GreatestPowerOpponentCreatureDiedEvent(
                        dyingEntityId = event.entityId,
                        dyingEntityName = event.entityName,
                        controllingPlayerId = controller,
                        observingPlayerId = observerId
                    )
                )
            }
        }

        return result
    }
}
