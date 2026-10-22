package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.permanent.attachments.TransformAttachToPlayer
import com.wingedsheep.engine.handlers.effects.permanent.types.TransformCopy
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.permanent.ProtectorAssignment
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent
import com.wingedsheep.sdk.core.Zone

class StateBasedContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(LegendRuleContinuation::class, ::resumeLegendRule),
        resumer(CommanderZoneChoiceContinuation::class, ::resumeCommanderZoneChoice),
        resumer(BattleProtectorChoiceContinuation::class, ::resumeBattleProtectorChoice),
        resumer(TransformAttachToPlayerContinuation::class, ::resumeTransformAttachToPlayer),
        resumer(TransformCopyContinuation::class, ::resumeTransformCopy),
    )

    /**
     * Attach the transformed Aura to the player its controller picked (CR 712.20), then settle any
     * other permanent that transformed into an attach-to-a-player face in the same batch before the
     * interrupted flow — the untap step, or the state-based action loop — carries on.
     */
    private fun resumeTransformAttachToPlayer(
        state: GameState,
        continuation: TransformAttachToPlayerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for attaching a transformed Aura")
        }
        val playerId = continuation.candidateIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid player option index ${response.optionIndex}")
        val (attached, events) = TransformAttachToPlayer.attach(state, continuation.auraId, playerId)
        val settled = TransformAttachToPlayer.settle(attached, services.cardRegistry, services.zones.predicateEvaluator, events)
        if (settled != null && settled.outcome !is Outcome.Done) return settled
        return checkForMore(settled?.state ?: attached, settled?.events ?: events)
    }

    /**
     * Make the transformed permanent a copy of the card its controller picked (CR 712.20), then
     * settle any other permanent that transformed into a becomes-a-copy face in the same batch before
     * the state-based action loop carries on.
     */
    private fun resumeTransformCopy(
        state: GameState,
        continuation: TransformCopyContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection for a transform copy")
        }
        val copiedId = response.selectedCards.singleOrNull()?.takeIf { it in continuation.candidateIds }
            ?: return ExecutionResult.error(state, "Choose exactly one of the offered cards to copy")
        val (copied, events) = TransformCopy.apply(
            state, continuation.permanentId, copiedId, services.cardRegistry, services.zones.predicateEvaluator
        )
        val settled = TransformCopy.settle(copied, services.cardRegistry, services.zones.predicateEvaluator, events)
        if (settled != null && settled.outcome !is Outcome.Done) return settled
        return checkForMore(settled?.state ?: copied, settled?.events ?: events)
    }

    /**
     * Apply the protector a battle's controller picked for the CR 704.5x/y state-based action.
     * The battle may have left the battlefield while the prompt was open (the SBA loop pauses
     * mid-pass), in which case there is nothing to assign and the loop simply carries on.
     */
    private fun resumeBattleProtectorChoice(
        state: GameState,
        continuation: BattleProtectorChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for battle protector choice")
        }
        val protectorId = continuation.candidateIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid protector option index ${response.optionIndex}")
        if (continuation.battleId !in state.getBattlefield()) {
            return checkForMore(state, emptyList())
        }
        val newState = ProtectorAssignment.assign(state, continuation.battleId, protectorId)
        return checkForMore(newState, emptyList())
    }

    private fun resumeLegendRule(
        state: GameState,
        continuation: LegendRuleContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for legend rule")
        }

        if (response.selectedCards.size != 1) {
            return ExecutionResult.error(state, "Must select exactly one legendary permanent to keep")
        }

        val keepEntityId = response.selectedCards.first()

        // Validate the selection is one of the duplicates
        if (keepEntityId !in continuation.allDuplicates) {
            return ExecutionResult.error(state, "Selected permanent is not one of the legendary duplicates")
        }

        // Put all other duplicates into graveyard
        val toRemove = continuation.allDuplicates.filter { it != keepEntityId }

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in toRemove) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val result = SbaZoneMovementHelper.putPermanentInGraveyard(services.zones, newState, entityId, cardComponent)
            newState = result.newState
            events.addAll(result.events)
        }

        return checkForMore(newState, events)
    }

    private fun resumeCommanderZoneChoice(
        state: GameState,
        continuation: CommanderZoneChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for commander zone choice")
        }

        // Re-check that the commander is still in the zone we asked about. Between the prompt
        // and the response another effect could in theory have moved it (engine pauses don't
        // typically allow that today, but this defends against future re-entrancy).
        val container = state.getEntity(continuation.commanderId)
            ?: return checkForMore(state, emptyList())
        val currentZone = state.zones.entries.firstOrNull { continuation.commanderId in it.value }?.key?.zoneType

        if (response.choice) {
            if (currentZone == null || currentZone == Zone.COMMAND) {
                return checkForMore(state, emptyList())
            }
            // Move to the command zone. ZoneTransitionService strips the asked marker on the
            // way (every commander zone change clears it), so the SBA will not re-prompt.
            val result = services.zones.moveToZone(
                state,
                continuation.commanderId,
                Zone.COMMAND,
            )
            return checkForMore(result.state, result.events)
        }

        // Decline: attach the asked marker so the SBA stops re-prompting on subsequent
        // iterations while the commander remains in this zone. The marker is cleared
        // automatically the next time the commander changes zones.
        val newState = state.updateEntity(continuation.commanderId) { c ->
            c.with(CommanderZoneChoiceAskedComponent)
        }
        return checkForMore(newState, emptyList())
    }
}
