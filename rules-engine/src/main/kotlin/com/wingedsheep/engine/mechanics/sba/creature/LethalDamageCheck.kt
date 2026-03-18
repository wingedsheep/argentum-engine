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

        for (entityId in state.getBattlefield().toList()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val damageComponent = container.get<DamageComponent>() ?: continue

            val projected = newState.projectedState
            if (!projected.isCreature(entityId)) continue

            if (projected.hasKeyword(entityId, Keyword.INDESTRUCTIBLE)) continue

            val effectiveToughness = projected.getToughness(entityId) ?: 0

            val hasLethalDamage = damageComponent.amount >= effectiveToughness
            val hasDeathtouch = damageComponent.deathtouchDamageReceived && damageComponent.amount > 0

            if (hasLethalDamage || hasDeathtouch) {
                // Check for regeneration shields
                val (shieldState, wasRegenerated) = ZoneMovementUtils.applyRegenerationShields(newState, entityId)
                if (wasRegenerated) {
                    val regenResult = ZoneMovementUtils.applyRegenerationReplacement(shieldState, entityId)
                    newState = regenResult.newState
                    events.addAll(regenResult.events)
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
