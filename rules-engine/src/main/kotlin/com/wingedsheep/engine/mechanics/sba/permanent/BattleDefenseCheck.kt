package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DefeatTriggerArmedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * CR 704.5v — if a battle has defense 0 and it isn't the source of an ability that has triggered
 * but not yet left the stack, it's put into its owner's graveyard. The battle analogue of
 * [PlaneswalkerLoyaltyCheck] (CR 704.5i), and it runs immediately after it for that reason.
 *
 * The "isn't the source of a triggered ability still on the stack" clause is what keeps a Siege
 * alive long enough for its own defeat trigger (CR 310.11b, "when the last defense counter is
 * removed from this permanent, exile it, then you may cast it transformed") to resolve and exile
 * it. Without the clause the battle would hit the graveyard first and the trigger would find
 * nothing to exile.
 */
class BattleDefenseCheck : StateBasedActionCheck {
    override val name = "704.5v Battle Defense"
    override val order = SbaOrder.BATTLE_DEFENSE

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            if (!Battles.isBattle(newState, entityId)) continue
            if (Battles.defenseOf(newState, entityId) > 0) continue
            if (isSourceOfPendingTriggeredAbility(newState, entityId)) continue

            // A Siege whose last defense counter was just removed by damage has *triggered* but
            // has not reached the stack yet — combat damage runs this check before the turn's
            // trigger-detection pass. Consume the marker and leave the battle alone for exactly
            // this pass; if the defeat trigger never appears (countered, or the permanent stopped
            // being a Siege) the next check finds no marker and bins it.
            if (container.has<DefeatTriggerArmedComponent>()) {
                newState = newState.updateEntity(entityId) { c -> c.without<DefeatTriggerArmedComponent>() }
                continue
            }

            val result = SbaZoneMovementHelper.putPermanentInGraveyard(newState, entityId, cardComponent)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * True while [entityId] is the source of an ability that has triggered but not yet left the
     * stack (CR 704.5v). Triggers are detected and pushed onto the stack by the trigger processor
     * as part of the action that emitted their event, so by the time state-based actions run the
     * Siege's defeat trigger is already a stack object — checking the stack is enough.
     */
    private fun isSourceOfPendingTriggeredAbility(
        state: GameState,
        entityId: com.wingedsheep.sdk.model.EntityId
    ): Boolean = state.stack.any { stackId ->
        state.getEntity(stackId)
            ?.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
            ?.sourceId == entityId
    }
}
