package com.wingedsheep.engine.mechanics.sba.creature

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword

/**
 * 704.5g - A creature that's been dealt lethal damage is destroyed.
 * 704.5h - A creature that's been dealt damage by a source with deathtouch is destroyed.
 * Note: Indestructible creatures are not destroyed by lethal damage (Rule 702.12b).
 * Creatures with regeneration shields are regenerated instead of destroyed.
 */
class LethalDamageCheck : StateBasedActionCheck {
    override val name = "704.5g/h Lethal Damage"
    override val order = SbaOrder.LETHAL_DAMAGE

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        // CR 704.3: state-based actions are checked, then all applicable ones are performed
        // simultaneously as a single event. The lethal-damage DETERMINATION must therefore be
        // made against a single projection taken from the original `state` before any creature
        // is moved — never re-projected off the progressively-mutated `newState`, which would
        // make the result depend on battlefield iteration order (e.g. an anti-lord giving
        // other creatures -2/-2 leaving before the small creature it was keeping lethal).
        // Mutation (regeneration / remove-damage shields / graveyard moves) still flows through
        // `newState`. Mirrors ZeroToughnessCheck.
        val projected = state.projectedState

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val damageComponent = container.get<DamageComponent>() ?: continue

            if (!projected.isCreature(entityId)) continue

            if (projected.hasKeyword(entityId, Keyword.INDESTRUCTIBLE)) continue

            val effectiveToughness = projected.getToughness(entityId) ?: 0

            val hasLethalDamage = damageComponent.amount >= effectiveToughness
            // CR 704.5h — destroyed if dealt damage by a deathtouch source, regardless of the
            // form that damage took. `deathtouchDamageReceived` is only ever set when a deathtouch
            // source actually dealt nonzero damage (marked, or as wither -1/-1 counters), so no
            // separate `amount > 0` guard is needed — and requiring marked `amount > 0` would
            // wrongly spare a creature whose deathtouch damage arrived as wither counters (which
            // are not marked damage, CR 702.80a).
            val hasDeathtouch = damageComponent.deathtouchDamageReceived

            if (hasLethalDamage || hasDeathtouch) {
                // Check for regeneration shields
                val (shieldState, wasRegenerated) = ZoneMovementUtils.applyRegenerationShields(newState, entityId)
                if (wasRegenerated) {
                    val regenResult = ZoneMovementUtils.applyRegenerationReplacement(shieldState, entityId)
                    newState = regenResult.newState
                    events.addAll(regenResult.events)
                    continue
                }

                // Check for remove-damage destruction shields (Pyramids). Its second mode
                // replaces "would be destroyed" with "remove all damage marked on it" — the
                // SBA destruction here is exactly that destruction, so the shield must fire
                // (an animated land taking lethal combat damage is the wording's intent).
                val (damageShieldState, wasShielded) = ZoneMovementUtils.applyRemoveDamageShields(newState, entityId)
                if (wasShielded) {
                    val shieldResult = ZoneMovementUtils.applyRemoveDamageReplacement(damageShieldState, entityId)
                    newState = shieldResult.newState
                    events.addAll(shieldResult.events)
                    continue
                }

                val result = SbaZoneMovementHelper.putCreatureInGraveyard(
                    newState, entityId, cardComponent, "lethal damage"
                )
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
