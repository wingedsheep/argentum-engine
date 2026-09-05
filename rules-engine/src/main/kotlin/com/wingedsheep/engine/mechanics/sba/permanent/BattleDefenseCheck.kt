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
 * CR 704.5v / 704.5w — a battle at defense 0 is put into its owner's graveyard. The battle analogue
 * of [PlaneswalkerLoyaltyCheck] (CR 704.5i), and it runs immediately after it for that reason.
 *
 * The August 7, 2026 rules update split this in two along the Siege line:
 *
 *  - **704.5v** — a *Siege* at 0 defense is binned only if it "isn't the source of an ability that
 *    has triggered but not yet left the stack". That clause is what keeps a Siege alive long enough
 *    for its own defeat trigger (CR 310.12b, "when the last defense counter is removed from this
 *    permanent, exile it, then you may cast it transformed") to resolve and exile it. Without it the
 *    battle would hit the graveyard first and the trigger would find nothing to exile.
 *  - **704.5w** — a *non-Siege* battle at 0 defense is binned with no such carve-out. It has no
 *    intrinsic defeat trigger to protect, so nothing has to survive to resolve.
 *
 * Before that update the carve-out was written for every battle, which was harmless while Siege was
 * the only printed battle type. CR 310.12 no longer says it is ("*Some* battles have the subtype
 * Siege", where it used to say "All currently existing battles have the subtype Siege"), so the
 * distinction is gated on [Battles.isSiege] rather than left to be discovered by the first non-Siege
 * battle that carries a triggered ability.
 */
class BattleDefenseCheck : StateBasedActionCheck {
    override val name = "704.5v/w Battle Defense"
    override val order = SbaOrder.BATTLE_DEFENSE

    override fun check(state: GameState): ExecutionResult = check(state, state, emptySet())

    override fun check(
        state: GameState,
        passStartState: GameState,
        pendingTriggerSources: Set<com.wingedsheep.engine.state.ObjectRef>
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            if (!Battles.isBattle(newState, entityId)) continue
            if (Battles.defenseOf(newState, entityId) > 0) continue

            // Both reprieves below are CR 704.5v's, and 704.5v is Siege-only. A non-Siege battle
            // falls through to 704.5w and is binned on the spot.
            if (Battles.isSiege(newState, entityId)) {
                if (isSourceOfPendingTriggeredAbility(newState, entityId, pendingTriggerSources)) continue

                // A Siege whose last defense counter was just removed by damage has *triggered* but
                // has not reached the stack yet — combat damage runs this check before the turn's
                // trigger-detection pass. Consume the marker and leave the battle alone for exactly
                // this pass; if the defeat trigger never appears (countered, or the permanent stopped
                // being a Siege) the next check finds no marker and bins it.
                if (container.has<DefeatTriggerArmedComponent>()) {
                    newState = newState.updateEntity(entityId) { c -> c.without<DefeatTriggerArmedComponent>() }
                    continue
                }
            }

            val result = SbaZoneMovementHelper.putPermanentInGraveyard(newState, entityId, cardComponent)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    /** Pending, decision-paused, and stacked triggers protect only their exact source object. */
    private fun isSourceOfPendingTriggeredAbility(
        state: GameState,
        entityId: com.wingedsheep.sdk.model.EntityId,
        pendingTriggerSources: Set<com.wingedsheep.engine.state.ObjectRef>
    ): Boolean {
        val current = state.objectRef(entityId) ?: return false
        if (current in pendingTriggerSources) return true
        if (state.stack.any { stackId ->
            state.getEntity(stackId)
                ?.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
                ?.objectReferences?.origin == current
        }) return true
        return state.continuationStack.any { frame ->
            when (frame) {
                is com.wingedsheep.engine.core.PendingTriggersContinuation ->
                    frame.remainingTriggers.any { it.objectReferences.origin == current }
                is com.wingedsheep.engine.core.TriggeredAbilityContinuation -> frame.objectReferences.origin == current
                is com.wingedsheep.engine.core.MayTriggerContinuation -> frame.trigger.objectReferences.origin == current
                is com.wingedsheep.engine.core.BatchMayTriggerContinuation ->
                    frame.triggers.any { it.objectReferences.origin == current }
                is com.wingedsheep.engine.core.MayPayManaTriggerContinuation -> frame.trigger.objectReferences.origin == current
                is com.wingedsheep.engine.core.ManaSourceSelectionContinuation -> frame.trigger.objectReferences.origin == current
                is com.wingedsheep.engine.core.TriggerModalModeSelectionContinuation -> frame.ability.objectReferences.origin == current
                is com.wingedsheep.engine.core.TriggerModalTargetSelectionContinuation -> frame.ability.objectReferences.origin == current
                else -> false
            }
        }
    }
}
