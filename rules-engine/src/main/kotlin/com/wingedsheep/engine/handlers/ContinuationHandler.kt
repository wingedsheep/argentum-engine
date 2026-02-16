package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.drawing.BlackmailExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachOpponentDiscardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsDrawsExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerDiscardsOrLoseLifeExecutor
import com.wingedsheep.engine.handlers.effects.drawing.EachPlayerMayDrawExecutor
import com.wingedsheep.engine.handlers.effects.drawing.ReadTheRunesExecutor
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.targeting.TargetOpponent
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 */
class ContinuationHandler(
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver = StackResolver(),
    private val triggerProcessor: com.wingedsheep.engine.event.TriggerProcessor? = null,
    private val triggerDetector: com.wingedsheep.engine.event.TriggerDetector? = null,
    private val combatManager: CombatManager? = null,
    private val targetFinder: TargetFinder = TargetFinder()
) {

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state with the pending decision already cleared
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult {
        val (continuation, stateAfterPop) = state.popContinuation()

        if (continuation == null) {
            // No continuation frame - this shouldn't happen if used correctly
            return ExecutionResult.success(state)
        }

        // Verify the decision ID matches
        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        return when (continuation) {
            is DiscardContinuation -> resumeDiscard(stateAfterPop, continuation, response)
            is ScryContinuation -> resumeScry(stateAfterPop, continuation, response)
            is SurveilContinuation -> resumeSurveil(stateAfterPop, continuation, response)
            is EffectContinuation -> resumeEffect(stateAfterPop, continuation, response)
            is TriggeredAbilityContinuation -> resumeTriggeredAbility(stateAfterPop, continuation, response)
            is DamageAssignmentContinuation -> resumeDamageAssignment(stateAfterPop, continuation, response)
            is ResolveSpellContinuation -> resumeSpellResolution(stateAfterPop, continuation, response)
            is SacrificeContinuation -> resumeSacrifice(stateAfterPop, continuation, response)
            is MayAbilityContinuation -> resumeMayAbility(stateAfterPop, continuation, response)
            is HandSizeDiscardContinuation -> resumeHandSizeDiscard(stateAfterPop, continuation, response)
            is EachPlayerSelectsThenDrawsContinuation -> resumeEachPlayerSelectsThenDraws(stateAfterPop, continuation, response)
            is EachPlayerDiscardsOrLoseLifeContinuation -> resumeEachPlayerDiscardsOrLoseLife(stateAfterPop, continuation, response)
            is ReturnFromGraveyardContinuation -> resumeReturnFromGraveyard(stateAfterPop, continuation, response)
            is SearchLibraryContinuation -> resumeSearchLibrary(stateAfterPop, continuation, response)
            is ReorderLibraryContinuation -> resumeReorderLibrary(stateAfterPop, continuation, response)
            is BlockerOrderContinuation -> resumeBlockerOrder(stateAfterPop, continuation, response)
            is EachPlayerChoosesDrawContinuation -> resumeEachPlayerChoosesDraw(stateAfterPop, continuation, response)
            is LookAtOpponentLibraryContinuation -> resumeLookAtOpponentLibrary(stateAfterPop, continuation, response)
            is ReorderOpponentLibraryContinuation -> resumeReorderOpponentLibrary(stateAfterPop, continuation, response)
            is PayOrSufferContinuation -> resumePayOrSuffer(stateAfterPop, continuation, response)
            is DistributeDamageContinuation -> resumeDistributeDamage(stateAfterPop, continuation, response)
            is LookAtTopCardsContinuation -> resumeLookAtTopCards(stateAfterPop, continuation, response)
            is RevealAndOpponentChoosesContinuation -> resumeRevealAndOpponentChooses(stateAfterPop, continuation, response)
            is ChooseColorProtectionContinuation -> resumeChooseColorProtection(stateAfterPop, continuation, response)
            is ChooseColorProtectionTargetContinuation -> resumeChooseColorProtectionTarget(stateAfterPop, continuation, response)
            is ChooseCreatureTypeReturnContinuation -> resumeChooseCreatureTypeReturn(stateAfterPop, continuation, response)
            is GraveyardToHandContinuation -> resumeGraveyardToHand(stateAfterPop, continuation, response)
            is ChooseFromCreatureTypeContinuation -> resumeChooseFromCreatureType(stateAfterPop, continuation, response)
            is ChooseToCreatureTypeContinuation -> resumeChooseToCreatureType(stateAfterPop, continuation, response)
            is PutFromHandContinuation -> resumePutFromHand(stateAfterPop, continuation, response)
            is UntapChoiceContinuation -> resumeUntapChoice(stateAfterPop, continuation, response)
            is BlackmailRevealContinuation -> resumeBlackmailReveal(stateAfterPop, continuation, response)
            is BlackmailChooseContinuation -> resumeBlackmailChoose(stateAfterPop, continuation, response)
            is HeadGamesContinuation -> resumeHeadGames(stateAfterPop, continuation, response)
            is ChooseCreatureTypeRevealTopContinuation -> resumeChooseCreatureTypeRevealTop(stateAfterPop, continuation, response)
            is RevealUntilCreatureTypeContinuation -> resumeRevealUntilCreatureType(stateAfterPop, continuation, response)
            is BecomeCreatureTypeContinuation -> resumeBecomeCreatureType(stateAfterPop, continuation, response)
            is ChooseCreatureTypeModifyStatsContinuation -> resumeChooseCreatureTypeModifyStats(stateAfterPop, continuation, response)
            is BecomeChosenTypeAllCreaturesContinuation -> resumeBecomeChosenTypeAllCreatures(stateAfterPop, continuation, response)
            is CounterUnlessPaysContinuation -> resumeCounterUnlessPays(stateAfterPop, continuation, response)
            is ChangeSpellTargetContinuation -> resumeChangeSpellTarget(stateAfterPop, continuation, response)
            is PutOnBottomOfLibraryContinuation -> resumePutOnBottomOfLibrary(stateAfterPop, continuation, response)
            is ModalContinuation -> resumeModal(stateAfterPop, continuation, response)
            is ModalTargetContinuation -> resumeModalTarget(stateAfterPop, continuation, response)
            is AnyPlayerMayPayContinuation -> resumeAnyPlayerMayPay(stateAfterPop, continuation, response)
            is MayPayManaContinuation -> resumeMayPayMana(stateAfterPop, continuation, response)
            is MayPayManaTriggerContinuation -> resumeMayPayManaTrigger(stateAfterPop, continuation, response)
            is ManaSourceSelectionContinuation -> resumeManaSourceSelection(stateAfterPop, continuation, response)
            is PendingTriggersContinuation -> {
                // This should not be popped directly by a decision response.
                // It's handled by checkForMoreContinuations after the preceding trigger resolves.
                ExecutionResult.error(state, "PendingTriggersContinuation should not be at top of stack during decision resume")
            }
            is MayTriggerContinuation -> resumeMayTrigger(stateAfterPop, continuation, response)
            is CloneEntersContinuation -> resumeCloneEnters(stateAfterPop, continuation, response)
            is ChooseColorEntersContinuation -> resumeChooseColorEnters(stateAfterPop, continuation, response)
            is ChooseCreatureTypeEntersContinuation -> resumeChooseCreatureTypeEnters(stateAfterPop, continuation, response)
            is CastWithCreatureTypeContinuation -> resumeCastWithCreatureType(stateAfterPop, continuation, response)
            is EachOpponentMayPutFromHandContinuation -> resumeEachOpponentMayPutFromHand(stateAfterPop, continuation, response)
            is ChooseAuraTargetForEntryFromHandContinuation -> resumeChooseAuraTargetForEntryFromHand(stateAfterPop, continuation, response)
            is EachPlayerMayRevealCreaturesContinuation -> resumeEachPlayerMayRevealCreatures(stateAfterPop, continuation, response)
            is EachPlayerSearchesLibraryContinuation -> resumeEachPlayerSearchesLibrary(stateAfterPop, continuation, response)
            is ChooseCreatureTypeMustAttackContinuation -> resumeChooseCreatureTypeMustAttack(stateAfterPop, continuation, response)
            is ChooseCreatureTypeUntapContinuation -> resumeChooseCreatureTypeUntap(stateAfterPop, continuation, response)
            is HarshMercyContinuation -> resumeHarshMercy(stateAfterPop, continuation, response)
            is PatriarchsBiddingContinuation -> resumePatriarchsBidding(stateAfterPop, continuation, response)
            is ChainCopyDecisionContinuation -> resumeChainCopyDecision(stateAfterPop, continuation, response)
            is ChainCopyTargetContinuation -> resumeChainCopyTarget(stateAfterPop, continuation, response)
            is BounceChainCopyDecisionContinuation -> resumeBounceChainCopyDecision(stateAfterPop, continuation, response)
            is BounceChainCopyLandContinuation -> resumeBounceChainCopyLand(stateAfterPop, continuation, response)
            is BounceChainCopyTargetContinuation -> resumeBounceChainCopyTarget(stateAfterPop, continuation, response)
            is DiscardForChainContinuation -> resumeDiscardForChain(stateAfterPop, continuation, response)
            is DiscardChainCopyDecisionContinuation -> resumeDiscardChainCopyDecision(stateAfterPop, continuation, response)
            is DiscardChainCopyTargetContinuation -> resumeDiscardChainCopyTarget(stateAfterPop, continuation, response)
            is DamageChainCopyDecisionContinuation -> resumeDamageChainCopyDecision(stateAfterPop, continuation, response)
            is DamageChainDiscardContinuation -> resumeDamageChainDiscard(stateAfterPop, continuation, response)
            is DamageChainCopyTargetContinuation -> resumeDamageChainCopyTarget(stateAfterPop, continuation, response)
            is DamagePreventionContinuation -> resumeDamagePrevention(stateAfterPop, continuation, response)
            is PreventDamageChainCopyDecisionContinuation -> resumePreventDamageChainCopyDecision(stateAfterPop, continuation, response)
            is PreventDamageChainCopyLandContinuation -> resumePreventDamageChainCopyLand(stateAfterPop, continuation, response)
            is PreventDamageChainCopyTargetContinuation -> resumePreventDamageChainCopyTarget(stateAfterPop, continuation, response)
            is SecretBidContinuation -> resumeSecretBid(stateAfterPop, continuation, response)
            is DrawReplacementBounceContinuation -> resumeDrawReplacementBounce(stateAfterPop, continuation, response)
            is DrawReplacementDiscardContinuation -> resumeDrawReplacementDiscard(stateAfterPop, continuation, response)
            is DrawReplacementActivationContinuation -> resumeDrawReplacementActivation(stateAfterPop, continuation, response)
            is ReadTheRunesContinuation -> resumeReadTheRunes(stateAfterPop, continuation, response)
        }
    }

    /**
     * Resume after player selected cards to discard.
     */
    private fun resumeDiscard(
        state: GameState,
        continuation: DiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Controller draws for each card discarded (Syphon Mind)
        if (continuation.controllerDrawsPerDiscard > 0 && continuation.controllerId != null) {
            val drawCount = selectedCards.size * continuation.controllerDrawsPerDiscard
            val drawResult = EachOpponentDiscardsExecutor.drawCards(
                newState, continuation.controllerId, drawCount
            )
            newState = drawResult.state
            events.addAll(drawResult.events)
        }

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player ordered cards for scry.
     */
    private fun resumeScry(
        state: GameState,
        continuation: ScryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is PilesSplitResponse) {
            return ExecutionResult.error(state, "Expected pile split response for scry")
        }

        val playerId = continuation.playerId

        // Pile 0 = top of library, Pile 1 = bottom of library
        val topCards = response.piles.getOrElse(0) { emptyList() }
        val bottomCards = response.piles.getOrElse(1) { emptyList() }

        var newState = state
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // Remove all scried cards from library first
        val allScried = topCards + bottomCards
        for (cardId in allScried) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // Get current library
        val currentLibrary = newState.getZone(libraryZone).toMutableList()

        // Add top cards to top of library (in order)
        val newLibrary = topCards + currentLibrary + bottomCards

        // Update zone with new order
        newState = newState.copy(
            zones = newState.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            ScryCompletedEvent(playerId, topCards.size, bottomCards.size)
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player split cards for surveil.
     * Pile 0 = top of library (in order), Pile 1 = graveyard.
     */
    private fun resumeSurveil(
        state: GameState,
        continuation: SurveilContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is PilesSplitResponse) {
            return ExecutionResult.error(state, "Expected pile split response for surveil")
        }

        val playerId = continuation.playerId

        // Pile 0 = top of library, Pile 1 = graveyard
        val topCards = response.piles.getOrElse(0) { emptyList() }
        val graveyardCards = response.piles.getOrElse(1) { emptyList() }

        var newState = state
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Remove all surveilled cards from library first
        val allSurveilled = topCards + graveyardCards
        for (cardId in allSurveilled) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // Put top cards back on top of library (in order)
        val currentLibrary = newState.getZone(libraryZone).toMutableList()
        val newLibrary = topCards + currentLibrary
        newState = newState.copy(
            zones = newState.zones + (libraryZone to newLibrary)
        )

        // Move graveyard cards to graveyard
        val events = mutableListOf<GameEvent>()
        for (cardId in graveyardCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.addToZone(graveyardZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume a composite effect with remaining effects.
     */
    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val context = continuation.toEffectContext()
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, effect) in continuation.remainingEffects.withIndex()) {
            val stillRemaining = continuation.remainingEffects.drop(index + 1)

            // Pre-push EffectContinuation for remaining effects BEFORE executing.
            // This ensures that if the sub-effect pushes its own continuation,
            // that continuation ends up on TOP (to be processed first when the response comes).
            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending", // Will be found by checkForMoreContinuations
                    remainingEffects = stillRemaining,
                    sourceId = continuation.sourceId,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    xValue = continuation.xValue
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, context)

            if (!result.isSuccess && !result.isPaused) {
                // Sub-effect failed - skip it and continue with remaining effects.
                // Per MTG rules, do as much as possible.
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                // Sub-effect paused. Its continuation is on top.
                // Our pre-pushed EffectContinuation is underneath, ready to be
                // processed by checkForMoreContinuations after the sub-effect resolves.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Effect succeeded - pop the pre-pushed continuation (it wasn't needed)
            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return checkForMoreContinuations(currentState, allEvents)
    }

    /**
     * Resume placing a triggered ability on the stack after target selection.
     *
     * The player has selected targets for a triggered ability. Now we can
     * actually put the ability on the stack with those targets.
     */
    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        // Convert the selected targets into ChosenTarget objects
        // The response contains a map of requirement index -> selected entity IDs
        // For most triggered abilities, there's a single requirement at index 0
        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId ->
                // Determine what kind of target this is based on the entity
                val container = state.getEntity(entityId)
                when {
                    // Check if it's a player
                    entityId in state.turnOrder -> ChosenTarget.Player(entityId)
                    // Check if it's on the battlefield (permanent)
                    entityId in state.getBattlefield() -> ChosenTarget.Permanent(entityId)
                    // Check if it's on the stack (spell)
                    entityId in state.stack -> ChosenTarget.Spell(entityId)
                    // Check if it's in a graveyard (card)
                    else -> {
                        // Look through all graveyards to find which player owns this card
                        val graveyardOwner = state.turnOrder.find { playerId ->
                            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                            entityId in state.getZone(graveyardZone)
                        }
                        if (graveyardOwner != null) {
                            ChosenTarget.Card(entityId, graveyardOwner, Zone.GRAVEYARD)
                        } else {
                            // Default to permanent (fallback for unknown cases)
                            ChosenTarget.Permanent(entityId)
                        }
                    }
                }
            }
        }

        // If player selected 0 targets for an optional ability (minTargets was 0),
        // they are declining the ability
        if (selectedTargets.isEmpty()) {
            // If the ability has an else effect (e.g., "If you don't, tap this creature"),
            // put it on the stack with the else effect
            if (continuation.elseEffect != null) {
                val elseComponent = TriggeredAbilityOnStackComponent(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    controllerId = continuation.controllerId,
                    effect = continuation.elseEffect,
                    description = continuation.description,
                    triggerDamageAmount = continuation.triggerDamageAmount,
                    triggeringEntityId = continuation.triggeringEntityId
                )
                val stackResult = stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
                if (!stackResult.isSuccess) return stackResult
                return checkForMoreContinuations(stackResult.newState, stackResult.events.toList())
            }
            return checkForMoreContinuations(state, emptyList())
        }

        // Create the triggered ability component to put on stack
        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId
        )

        // Put the ability on the stack with the selected targets
        val stackResult = stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        // After putting the ability on stack, check for more continuations
        return checkForMoreContinuations(stackResult.newState, stackResult.events.toList())
    }

    /**
     * Resume after player answered yes/no for a may-trigger-with-targets ability.
     *
     * If the player says yes, unwrap the MayEffect and proceed to target selection
     * with the inner effect. If the player says no, skip the trigger entirely.
     */
    private fun resumeMayTrigger(
        state: GameState,
        continuation: MayTriggerContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may trigger")
        }

        if (!response.choice) {
            // Player declined - skip the trigger entirely
            return checkForMoreContinuations(state, emptyList())
        }

        // Player said yes - unwrap the MayEffect and proceed to target selection
        val trigger = continuation.trigger
        val mayEffect = trigger.ability.effect as com.wingedsheep.sdk.scripting.MayEffect
        val innerEffect = mayEffect.effect

        // Create a modified trigger with the inner effect (no MayEffect wrapper)
        // so that when the ability resolves, it executes directly without another yes/no
        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        // Now process as a normal targeted trigger
        val processor = triggerProcessor
            ?: return ExecutionResult.error(state, "TriggerProcessor not available for may trigger continuation")

        val result = processor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            // Target selection is needed - return paused directly
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        // Target was auto-selected - check for more continuations
        return checkForMoreContinuations(result.newState, result.events.toList())
    }

    /**
     * Resume combat damage assignment.
     *
     * Accepts both DistributionResponse (from DivideCombatDamageFreely / Butcher Orgg)
     * and DamageAssignmentResponse (from regular blocked attacker damage assignment).
     *
     * Stores the player's chosen distribution on the attacker entity and
     * re-runs applyCombatDamage. If another attacker also needs a decision,
     * the result will be paused again; otherwise all combat damage is applied.
     */
    private fun resumeDamageAssignment(
        state: GameState,
        continuation: DamageAssignmentContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val assignments = when (response) {
            is DistributionResponse -> response.distribution
            is DamageAssignmentResponse -> response.assignments
            else -> return ExecutionResult.error(state, "Expected distribution or damage assignment response")
        }

        // Store the distribution as DamageAssignmentComponent on the attacker
        val newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(
                    assignments
                )
            )
        }

        // Re-run combat damage — this attacker now has an assignment and will be skipped
        // in the pre-check. If another attacker needs a decision, it will pause again.
        return combatManager?.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
            ?: ExecutionResult.success(newState)
    }

    /**
     * Resume after defender distributes damage prevention among multiple combat damage sources.
     *
     * Removes the original prevention shield and creates per-source shields based on the
     * player's allocation, then re-runs combat damage so the normal damage loop uses them.
     */
    private fun resumeDamagePrevention(
        state: GameState,
        continuation: DamagePreventionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for damage prevention")
        }

        // Remove the original shield
        val updatedEffects = state.floatingEffects.toMutableList()
        val shieldIndex = updatedEffects.indexOfFirst { it.id == continuation.shieldEffectId }
        val originalShield = if (shieldIndex >= 0) updatedEffects.removeAt(shieldIndex) else null

        // Create per-source prevention shields based on the player's allocation
        val timestamp = originalShield?.timestamp ?: System.currentTimeMillis()
        for ((sourceId, preventionAmount) in response.distribution) {
            if (preventionAmount <= 0) continue
            updatedEffects.add(
                ActiveFloatingEffect(
                    id = EntityId(java.util.UUID.randomUUID().toString()),
                    effect = FloatingEffectData(
                        layer = Layer.ABILITY,
                        modification = SerializableModification.PreventNextDamage(preventionAmount, onlyFromSource = sourceId),
                        affectedEntities = setOf(continuation.recipientId)
                    ),
                    duration = originalShield?.duration ?: com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
                    sourceId = originalShield?.sourceId,
                    sourceName = originalShield?.sourceName,
                    controllerId = originalShield?.controllerId ?: continuation.recipientId,
                    timestamp = timestamp
                )
            )
        }

        val newState = state.copy(floatingEffects = updatedEffects)

        // Re-run combat damage — the per-source shields will be consumed by the normal damage loop
        return combatManager?.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
            ?: ExecutionResult.success(newState)
    }

    /**
     * Resume spell resolution after target/mode selection.
     */
    private fun resumeSpellResolution(
        state: GameState,
        continuation: ResolveSpellContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        // TODO: Implement spell resolution resumption
        // This would store targets/modes and continue resolution
        return ExecutionResult.success(state)
    }

    /**
     * Resume after player selected a card from their graveyard.
     */
    private fun resumeReturnFromGraveyard(
        state: GameState,
        continuation: ReturnFromGraveyardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for graveyard search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Empty selection — no card returned
        if (selectedCards.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardId = selectedCards.first()
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Validate card is still in graveyard
        if (cardId !in state.getZone(graveyardZone)) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        var newState = state.removeFromZone(graveyardZone, cardId)

        val toZone = when (continuation.destination) {
            SearchDestination.HAND -> {
                val handZone = ZoneKey(playerId, Zone.HAND)
                newState = newState.addToZone(handZone, cardId)
                Zone.HAND
            }
            SearchDestination.BATTLEFIELD -> {
                val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
                newState = newState.addToZone(battlefieldZone, cardId)
                newState = newState.updateEntity(cardId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))
                        .with(com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent)
                }
                Zone.BATTLEFIELD
            }
            else -> return ExecutionResult.error(state, "Unsupported destination: ${continuation.destination}")
        }

        val events = listOf(
            ZoneChangeEvent(
                entityId = cardId,
                entityName = cardName,
                fromZone = Zone.GRAVEYARD,
                toZone = toZone,
                ownerId = playerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards from library search.
     *
     * Handles:
     * 1. "Fail to find" (0 cards selected) - just shuffle if configured
     * 2. Move selected cards from library to destination zone
     * 3. Apply tapped status if entering battlefield tapped
     * 4. Emit reveal events if configured
     * 5. Shuffle library if configured
     */
    private fun resumeSearchLibrary(
        state: GameState,
        continuation: SearchLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for library search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Handle "fail to find" case - just shuffle and return
        if (selectedCards.isEmpty()) {
            if (continuation.shuffleAfter) {
                val library = newState.getZone(libraryZone).shuffled()
                newState = newState.copy(zones = newState.zones + (libraryZone to library))
                events.add(LibraryShuffledEvent(playerId))
            }
            return checkForMoreContinuations(newState, events)
        }

        // Remove selected cards from library
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // For TOP_OF_LIBRARY: shuffle FIRST, then put cards on top
        // This matches oracle text like "shuffle your library, then put the card on top"
        if (continuation.destination == SearchDestination.TOP_OF_LIBRARY) {
            // Shuffle library first (without the selected cards)
            if (continuation.shuffleAfter) {
                val library = newState.getZone(libraryZone).shuffled()
                newState = newState.copy(zones = newState.zones + (libraryZone to library))
                events.add(LibraryShuffledEvent(playerId))
            }

            // Then put the cards on top
            val currentLibrary = newState.getZone(libraryZone)
            newState = newState.copy(
                zones = newState.zones + (libraryZone to selectedCards + currentLibrary)
            )

            // Emit zone change events
            val cardNames = mutableListOf<String>()
            val imageUris = mutableListOf<String?>()
            for (cardId in selectedCards) {
                val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
                val cardName = cardComponent?.name ?: "Unknown"
                cardNames.add(cardName)
                imageUris.add(cardComponent?.imageUri)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.LIBRARY,
                        ownerId = playerId
                    )
                )
            }

            // Reveal cards if configured
            if (continuation.reveal) {
                events.add(
                    CardsRevealedEvent(
                        revealingPlayerId = playerId,
                        cardIds = selectedCards,
                        cardNames = cardNames,
                        imageUris = imageUris,
                        source = continuation.sourceName
                    )
                )
            }

            return checkForMoreContinuations(newState, events)
        }

        // For other destinations: move cards, then shuffle
        val destinationZone = when (continuation.destination) {
            SearchDestination.HAND -> ZoneKey(playerId, Zone.HAND)
            SearchDestination.BATTLEFIELD -> ZoneKey(playerId, Zone.BATTLEFIELD)
            SearchDestination.GRAVEYARD -> ZoneKey(playerId, Zone.GRAVEYARD)
            SearchDestination.TOP_OF_LIBRARY -> libraryZone // unreachable
        }

        val cardNames = mutableListOf<String>()
        val imageUris = mutableListOf<String?>()
        for (cardId in selectedCards) {
            newState = newState.addToZone(destinationZone, cardId)

            // Apply battlefield-specific components
            if (continuation.destination == SearchDestination.BATTLEFIELD) {
                val container = newState.getEntity(cardId)
                if (container != null) {
                    var newContainer = container
                        .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))

                    // Creatures enter with summoning sickness
                    val cardComponent = container.get<CardComponent>()
                    if (cardComponent?.typeLine?.isCreature == true) {
                        newContainer = newContainer.with(
                            com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                        )
                    }

                    // Apply tapped status if entersTapped
                    if (continuation.entersTapped) {
                        newContainer = newContainer.with(TappedComponent)
                    }

                    newState = newState.copy(
                        entities = newState.entities + (cardId to newContainer)
                    )
                }
            }

            // Emit zone change event
            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            val cardName = cardComponent?.name ?: "Unknown"
            cardNames.add(cardName)
            imageUris.add(cardComponent?.imageUri)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = when (continuation.destination) {
                        SearchDestination.HAND -> Zone.HAND
                        SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD
                        SearchDestination.GRAVEYARD -> Zone.GRAVEYARD
                        SearchDestination.TOP_OF_LIBRARY -> Zone.LIBRARY
                    },
                    ownerId = playerId
                )
            )
        }

        // Reveal cards if configured
        if (continuation.reveal) {
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = selectedCards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = continuation.sourceName
                )
            )
        }

        // Shuffle library after search if configured
        if (continuation.shuffleAfter) {
            val library = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(zones = newState.zones + (libraryZone to library))
            events.add(LibraryShuffledEvent(playerId))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player reordered cards on top of their library.
     *
     * The response contains the card IDs in the new order (first = new top of library).
     * We replace the top N cards in the library with this new order.
     */
    private fun resumeReorderLibrary(
        state: GameState,
        continuation: ReorderLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for library reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they were at the top)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards back on top in the new order
        val newLibrary = orderedCards + remainingLibrary

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            LibraryReorderedEvent(
                playerId = playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player ordered cards to put on the bottom of their library.
     *
     * Same as resumeReorderLibrary but places cards on the BOTTOM of the library
     * instead of the top. Used for effects like Erratic Explosion.
     */
    private fun resumePutOnBottomOfLibrary(
        state: GameState,
        continuation: PutOnBottomOfLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for library bottom reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they should already be removed by the executor,
        // but filter just in case)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards on the BOTTOM in the player's chosen order
        val newLibrary = remainingLibrary + orderedCards

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            LibraryReorderedEvent(
                playerId = playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after attacking player declared damage assignment order for blockers.
     *
     * Per MTG CR 509.2, the attacking player must order blockers for each attacker
     * blocked by 2+ creatures. This determines the order damage is assigned.
     */
    private fun resumeBlockerOrder(
        state: GameState,
        continuation: BlockerOrderContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for blocker ordering")
        }

        // Add DamageAssignmentOrderComponent to the attacker
        var newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(
                    response.orderedObjects
                )
            )
        }

        val events = mutableListOf<GameEvent>(
            BlockerOrderDeclaredEvent(continuation.attackerId, response.orderedObjects)
        )

        // Check if there are more attackers that need ordering
        if (continuation.remainingAttackers.isNotEmpty()) {
            val nextAttacker = continuation.remainingAttackers.first()
            val nextRemaining = continuation.remainingAttackers.drop(1)

            // Create decision for next attacker
            val attackerContainer = newState.getEntity(nextAttacker)!!
            val attackerCard = attackerContainer.get<CardComponent>()!!
            val blockedComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()!!
            val blockerIds = blockedComponent.blockerIds

            // Build card info for blockers
            // Face-down creatures must not reveal their identity
            val cardInfo = blockerIds.associateWith { blockerId ->
                val blockerContainer = newState.getEntity(blockerId)
                val isFaceDown = blockerContainer?.has<FaceDownComponent>() == true
                if (isFaceDown) {
                    SearchCardInfo(
                        name = "Morph",
                        manaCost = "{3}",
                        typeLine = "Creature"
                    )
                } else {
                    val blockerCard = blockerContainer?.get<CardComponent>()
                    SearchCardInfo(
                        name = blockerCard?.name ?: "Unknown",
                        manaCost = blockerCard?.manaCost?.toString() ?: "",
                        typeLine = blockerCard?.typeLine?.toString() ?: ""
                    )
                }
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = OrderObjectsDecision(
                id = decisionId,
                playerId = continuation.attackingPlayerId,
                prompt = "Order damage assignment for ${attackerCard.name}",
                context = DecisionContext(
                    sourceId = nextAttacker,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                objects = blockerIds,
                cardInfo = cardInfo
            )

            val nextContinuation = BlockerOrderContinuation(
                decisionId = decisionId,
                attackingPlayerId = continuation.attackingPlayerId,
                attackerId = nextAttacker,
                attackerName = attackerCard.name,
                remainingAttackers = nextRemaining
            )

            newState = newState
                .withPendingDecision(decision)
                .pushContinuation(nextContinuation)

            events.add(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = continuation.attackingPlayerId,
                    decisionType = "ORDER_BLOCKERS",
                    prompt = decision.prompt
                )
            )

            return ExecutionResult.paused(newState, decision, events)
        }

        // All attackers have been ordered - give priority back to the active player
        // Per MTG CR 509.4, after blockers are declared (and ordered), players get priority
        // in the declare blockers step before combat damage happens
        val activePlayer = newState.activePlayerId
        val stateWithPriority = if (activePlayer != null) {
            newState.withPriority(activePlayer)
        } else {
            newState
        }
        return checkForMoreContinuations(stateWithPriority, events)
    }

    /**
     * Resume after player selected cards to sacrifice.
     */
    private fun resumeSacrifice(
        state: GameState,
        continuation: SacrificeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // Move selected permanents from battlefield to owner's graveyard
        var newState = state
        val events = mutableListOf<GameEvent>()

        if (selectedPermanents.isNotEmpty()) {
            events.add(PermanentsSacrificedEvent(playerId, selectedPermanents))
        }

        for (permanentId in selectedPermanents) {
            val container = newState.getEntity(permanentId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Find the actual zone (may differ from controller's zone for stolen creatures)
            val currentZone = newState.zones.entries.find { (_, cards) -> permanentId in cards }?.key
                ?: continue

            // Sacrificed cards go to their owner's graveyard
            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: playerId
            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

            newState = newState.removeFromZone(currentZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)

            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = cardComponent.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = ownerId
                )
            )
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards to discard for hand size during cleanup.
     */
    private fun resumeHandSizeDiscard(
        state: GameState,
        continuation: HandSizeDiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for hand size discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player made a yes/no choice.
     */
    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.toEffectContext()
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            // No effect to execute (player said no and there's no alternative)
            return checkForMoreContinuations(state, emptyList())
        }

        val result = effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            // The effect needs another decision
            return result
        }

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after the spell's controller decides whether to pay to prevent countering.
     */
    private fun resumeCounterUnlessPays(
        state: GameState,
        continuation: CounterUnlessPaysContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for counter unless pays")
        }

        if (response.choice) {
            // Player chose to pay — auto-tap sources and deduct mana
            val playerId = continuation.payingPlayerId
            val playerEntity = state.getEntity(playerId)
                ?: return ExecutionResult.error(state, "Paying player not found")

            val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
                ?: return ExecutionResult.error(state, "Player has no mana pool")

            val manaPool = ManaPool(
                manaPoolComponent.white,
                manaPoolComponent.blue,
                manaPoolComponent.black,
                manaPoolComponent.red,
                manaPoolComponent.green,
                manaPoolComponent.colorless
            )

            // Try to pay from floating mana first, then tap sources for the rest
            val partialResult = manaPool.payPartial(continuation.manaCost)
            val remainingCost = partialResult.remainingCost
            var currentPool = manaPool
            var currentState = state
            val events = mutableListOf<GameEvent>()

            if (!remainingCost.isEmpty()) {
                // Need to tap sources for the remaining cost
                val manaSolver = ManaSolver()
                val solution = manaSolver.solve(currentState, playerId, remainingCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost")

                // Tap sources and add their mana to the pool
                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }

                for ((_, production) in solution.manaProduced) {
                    currentPool = if (production.color != null) {
                        currentPool.add(production.color)
                    } else {
                        currentPool.addColorless(production.colorless)
                    }
                }
            }

            // Deduct the cost from the pool
            val newPool = currentPool.pay(continuation.manaCost)
                ?: return ExecutionResult.error(state, "Cannot pay mana cost after auto-tap")

            currentState = currentState.updateEntity(playerId) { container ->
                container.with(
                    ManaPoolComponent(
                        white = newPool.white,
                        blue = newPool.blue,
                        black = newPool.black,
                        red = newPool.red,
                        green = newPool.green,
                        colorless = newPool.colorless
                    )
                )
            }

            // Spell resolves normally — don't counter it
            return checkForMoreContinuations(currentState, events)
        } else {
            // Player chose not to pay — counter the spell
            val counterResult = stackResolver.counterSpell(state, continuation.spellEntityId)
            return checkForMoreContinuations(counterResult.newState, counterResult.events)
        }
    }

    /**
     * Resume after Meddle's controller chooses a new creature target for a spell.
     */
    private fun resumeChangeSpellTarget(
        state: GameState,
        continuation: ChangeSpellTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for change spell target")
        }

        val selectedCreatureId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No creature selected for change spell target")

        // Get the spell entity and update its target
        val spellEntity = state.getEntity(continuation.spellEntityId)
            ?: return checkForMoreContinuations(state, emptyList()) // Spell no longer on stack

        val targetsComponent = spellEntity.get<TargetsComponent>()
            ?: return checkForMoreContinuations(state, emptyList())

        // Replace the single target with the new creature
        val newTargets = listOf(ChosenTarget.Permanent(selectedCreatureId))
        val updatedState = state.updateEntity(continuation.spellEntityId) { container ->
            container.with(TargetsComponent(newTargets, targetsComponent.targetRequirements))
        }

        return checkForMoreContinuations(updatedState, emptyList())
    }

    /**
     * Resume after the controller decides whether to pay a mana cost for "you may pay {cost}" effects.
     */
    private fun resumeMayPayMana(
        state: GameState,
        continuation: MayPayManaContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may pay mana")
        }

        if (!response.choice) {
            // Player declined to pay — nothing happens
            return checkForMoreContinuations(state, emptyList())
        }

        // Player chose to pay — auto-tap sources and deduct mana
        val playerId = continuation.playerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        // Try to pay from floating mana first, then tap sources for the rest
        val partialResult = manaPool.payPartial(continuation.manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            // Need to tap sources for the remaining cost
            val manaSolver = ManaSolver()
            val solution = manaSolver.solve(currentState, playerId, remainingCost)
                ?: return ExecutionResult.error(state, "Cannot pay mana cost")

            // Tap sources and add their mana to the pool
            for (source in solution.sources) {
                currentState = currentState.updateEntity(source.entityId) { c ->
                    c.with(TappedComponent)
                }
                events.add(TappedEvent(source.entityId, source.name))
            }

            for ((_, production) in solution.manaProduced) {
                currentPool = if (production.color != null) {
                    currentPool.add(production.color)
                } else {
                    currentPool.addColorless(production.colorless)
                }
            }
        }

        // Deduct the cost from the pool
        val newPool = currentPool.pay(continuation.manaCost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after auto-tap")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        // Execute the inner effect
        val context = continuation.toEffectContext()
        val effectResult = effectExecutorRegistry.execute(currentState, continuation.effect, context)

        if (effectResult.error != null) {
            return effectResult
        }

        val allEvents = events + effectResult.events
        return checkForMoreContinuations(effectResult.state, allEvents)
    }

    /**
     * Resume after the controller decides whether to pay a mana cost for a triggered
     * ability that also requires targets (e.g., Lightning Rift).
     *
     * If the player says yes, shows mana source selection. If no, skips the trigger.
     */
    private fun resumeMayPayManaTrigger(
        state: GameState,
        continuation: MayPayManaTriggerContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may pay mana trigger")
        }

        if (!response.choice) {
            // Player declined to pay — skip the trigger entirely
            return checkForMoreContinuations(state, emptyList())
        }

        // Player chose to pay — show mana source selection
        val playerId = continuation.trigger.controllerId
        val manaSolver = ManaSolver()

        // Find available sources for the UI
        val sources = manaSolver.findAvailableManaSources(state, playerId)
        val sourceOptions = sources.map { source ->
            ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless
            )
        }

        // Get auto-pay suggestion
        val solution = manaSolver.solve(state, playerId, continuation.manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        // Create mana source selection decision
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectManaSourcesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay ${continuation.manaCost}",
            context = DecisionContext(
                sourceId = continuation.trigger.sourceId,
                sourceName = continuation.trigger.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            availableSources = sourceOptions,
            requiredCost = continuation.manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion
        )

        val manaSourceContinuation = ManaSourceSelectionContinuation(
            decisionId = decisionId,
            trigger = continuation.trigger,
            targetRequirement = continuation.targetRequirement,
            manaCost = continuation.manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(manaSourceContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_MANA_SOURCES",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the controller selects mana sources to pay a cost for a triggered
     * ability that also requires targets.
     *
     * Taps the selected sources, deducts mana, unwraps MayPayManaEffect, and proceeds
     * to target selection with the inner effect.
     */
    private fun resumeManaSourceSelection(
        state: GameState,
        continuation: ManaSourceSelectionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response")
        }

        val playerId = continuation.trigger.controllerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        // Try to pay from floating mana first
        val partialResult = manaPool.payPartial(continuation.manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                // Auto-tap: use ManaSolver
                val manaSolver = ManaSolver()
                val solution = manaSolver.solve(currentState, playerId, remainingCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost with auto-pay")

                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }

                for ((_, production) in solution.manaProduced) {
                    currentPool = if (production.color != null) {
                        currentPool.add(production.color)
                    } else {
                        currentPool.addColorless(production.colorless)
                    }
                }
            } else {
                // Manual selection: tap the selected sources
                // Use the available sources stored in the continuation (already validated when decision was created)
                val sourceMap = continuation.availableSources.associateBy { it.entityId }

                for (sourceId in response.selectedSources) {
                    val source = sourceMap[sourceId]
                        ?: return ExecutionResult.error(state, "Selected source $sourceId is not a valid mana source")

                    currentState = currentState.updateEntity(sourceId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(sourceId, source.name))

                    // Add mana from this source to the pool
                    // For simplicity, produce the first color or colorless
                    if (source.producesColors.isNotEmpty()) {
                        currentPool = currentPool.add(source.producesColors.first())
                    } else if (source.producesColorless) {
                        currentPool = currentPool.addColorless(1)
                    }
                }
            }
        }

        // Deduct the cost from the pool
        val newPool = currentPool.pay(continuation.manaCost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after tapping sources")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        // Unwrap MayPayManaEffect to get inner effect
        val trigger = continuation.trigger
        val mayPayEffect = trigger.ability.effect as com.wingedsheep.sdk.scripting.MayPayManaEffect
        val innerEffect = mayPayEffect.effect

        // Create a modified trigger with the inner effect (mana already paid)
        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        // Proceed to target selection
        val processor = triggerProcessor
            ?: return ExecutionResult.error(state, "TriggerProcessor not available for mana source selection continuation")

        val result = processor.processTargetedTrigger(currentState, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            // Target selection is needed - return paused with accumulated events
            return ExecutionResult.paused(
                result.state,
                result.pendingDecision!!,
                events + result.events
            )
        }

        if (!result.isSuccess) {
            return result
        }

        // Target was auto-selected - check for more continuations
        return checkForMoreContinuations(result.newState, events + result.events.toList())
    }

    /**
     * Resume after a player selected cards for "each player selects, then draws" effects.
     * Handles effects like Flux where each player discards, then draws that many.
     */
    private fun resumeEachPlayerSelectsThenDraws(
        state: GameState,
        continuation: EachPlayerSelectsThenDrawsContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response")
        }

        val selectedCards = response.selectedCards

        // Get current player from the continuation
        val currentPlayerId = continuation.currentPlayerId

        // Discard the selected cards
        var newState = state
        val handZone = ZoneKey(currentPlayerId, Zone.HAND)
        val graveyardZone = ZoneKey(currentPlayerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardEvents = if (selectedCards.isNotEmpty()) {
            listOf(CardsDiscardedEvent(currentPlayerId, selectedCards))
        } else {
            emptyList()
        }

        // Update draw amounts with this player's count
        val newDrawAmounts = continuation.drawAmounts + (currentPlayerId to selectedCards.size)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            // Ask next player
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
            val nextHand = newState.getZone(nextHandZone)

            // Determine selection bounds for next player
            val minSelection = continuation.minSelection
            val maxSelection = (continuation.maxSelection ?: nextHand.size).coerceAtMost(nextHand.size)

            // If next player has empty hand or must select 0, skip them
            if (nextHand.isEmpty() || (minSelection == 0 && maxSelection == 0)) {
                // Skip this player, add 0 to their draw amount
                val skippedDrawAmounts = newDrawAmounts + (nextPlayer to 0)

                // Recursively check remaining players
                return continueEachPlayerSelection(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        drawAmounts = skippedDrawAmounts
                    ),
                    discardEvents
                )
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = continuation.selectionPrompt,
                options = nextHand,
                minSelections = minSelection,
                maxSelections = maxSelection,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            // Push updated continuation
            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                drawAmounts = newDrawAmounts
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                discardEvents + decisionResult.events
            )
        }

        // All players have selected - execute draws
        val drawResult = EachPlayerDiscardsDrawsExecutor.executeDraws(
            newState,
            newDrawAmounts,
            continuation.controllerBonusDraw,
            continuation.controllerId
        )

        return ExecutionResult(
            drawResult.state,
            discardEvents + drawResult.events,
            drawResult.error
        )
    }

    /**
     * Continue processing remaining players for each-player selection effects.
     * Handles skipping players with empty hands.
     */
    private fun continueEachPlayerSelection(
        state: GameState,
        continuation: EachPlayerSelectsThenDrawsContinuation,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        if (continuation.remainingPlayers.isEmpty()) {
            // All done - execute draws
            val drawResult = EachPlayerDiscardsDrawsExecutor.executeDraws(
                state,
                continuation.drawAmounts,
                continuation.controllerBonusDraw,
                continuation.controllerId
            )
            return ExecutionResult(
                drawResult.state,
                priorEvents + drawResult.events,
                drawResult.error
            )
        }

        val nextPlayer = continuation.remainingPlayers.first()
        val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

        val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
        val nextHand = state.getZone(nextHandZone)

        val minSelection = continuation.minSelection
        val maxSelection = (continuation.maxSelection ?: nextHand.size).coerceAtMost(nextHand.size)

        // If next player has empty hand or must select 0, skip them
        if (nextHand.isEmpty() || (minSelection == 0 && maxSelection == 0)) {
            val skippedDrawAmounts = continuation.drawAmounts + (nextPlayer to 0)
            return continueEachPlayerSelection(
                state,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    drawAmounts = skippedDrawAmounts
                ),
                priorEvents
            )
        }

        // Create decision for next player
        val decisionHandler = DecisionHandler()
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = nextPlayer,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            prompt = continuation.selectionPrompt,
            options = nextHand,
            minSelections = minSelection,
            maxSelections = maxSelection,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val newContinuation = continuation.copy(
            decisionId = decisionResult.pendingDecision!!.id,
            currentPlayerId = nextPlayer,
            remainingPlayers = nextRemainingPlayers
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            priorEvents + decisionResult.events
        )
    }

    /**
     * Resume after a player selected a card to discard for "each player discards or lose life" effects.
     *
     * Used for Strongarm Tactics: each player discards a card, then each player who
     * didn't discard a creature card loses N life.
     */
    private fun resumeEachPlayerDiscardsOrLoseLife(
        state: GameState,
        continuation: EachPlayerDiscardsOrLoseLifeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response")
        }

        val selectedCards = response.selectedCards
        val currentPlayerId = continuation.currentPlayerId

        // Discard the selected card
        var newState = state
        val handZone = ZoneKey(currentPlayerId, Zone.HAND)
        val graveyardZone = ZoneKey(currentPlayerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val discardEvents: List<GameEvent> = if (selectedCards.isNotEmpty()) {
            listOf(CardsDiscardedEvent(currentPlayerId, selectedCards))
        } else {
            emptyList()
        }

        // Check if the discarded card was a creature
        val discardedCreatureCard = selectedCards.any { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
        }

        val newDiscardedCreature = continuation.discardedCreature + (currentPlayerId to discardedCreatureCard)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
            val nextHand = newState.getZone(nextHandZone)

            // If next player has empty hand, skip them (didn't discard a creature)
            if (nextHand.isEmpty()) {
                val skippedDiscardedCreature = newDiscardedCreature + (nextPlayer to false)
                return continueEachPlayerDiscardsOrLoseLife(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        discardedCreature = skippedDiscardedCreature
                    ),
                    discardEvents
                )
            }

            // If next player has exactly 1 card, auto-discard
            if (nextHand.size == 1) {
                val cardId = nextHand.first()
                val isCreature = newState.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
                val nextGraveyardZone = ZoneKey(nextPlayer, Zone.GRAVEYARD)
                newState = newState.removeFromZone(nextHandZone, cardId)
                newState = newState.addToZone(nextGraveyardZone, cardId)

                val autoDiscardEvents = discardEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId)))
                val autoDiscardedCreature = newDiscardedCreature + (nextPlayer to isCreature)

                return continueEachPlayerDiscardsOrLoseLife(
                    newState,
                    continuation.copy(
                        remainingPlayers = nextRemainingPlayers,
                        discardedCreature = autoDiscardedCreature
                    ),
                    autoDiscardEvents
                )
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = "Choose a card to discard",
                options = nextHand,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                discardedCreature = newDiscardedCreature
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                discardEvents + decisionResult.events
            )
        }

        // All players have discarded - apply life loss
        val lifeLossResult = EachPlayerDiscardsOrLoseLifeExecutor.applyLifeLoss(
            newState, newDiscardedCreature, continuation.lifeLoss
        )

        return ExecutionResult(
            lifeLossResult.state,
            discardEvents + lifeLossResult.events,
            lifeLossResult.error
        )
    }

    /**
     * Continue processing remaining players for each-player discard-or-lose-life effects.
     * Handles skipping players with empty hands.
     */
    private fun continueEachPlayerDiscardsOrLoseLife(
        state: GameState,
        continuation: EachPlayerDiscardsOrLoseLifeContinuation,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        if (continuation.remainingPlayers.isEmpty()) {
            // All done - apply life loss
            val lifeLossResult = EachPlayerDiscardsOrLoseLifeExecutor.applyLifeLoss(
                state, continuation.discardedCreature, continuation.lifeLoss
            )
            return ExecutionResult(
                lifeLossResult.state,
                priorEvents + lifeLossResult.events,
                lifeLossResult.error
            )
        }

        val nextPlayer = continuation.remainingPlayers.first()
        val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

        val nextHandZone = ZoneKey(nextPlayer, Zone.HAND)
        val nextHand = state.getZone(nextHandZone)

        // If next player has empty hand, skip them
        if (nextHand.isEmpty()) {
            val skippedDiscardedCreature = continuation.discardedCreature + (nextPlayer to false)
            return continueEachPlayerDiscardsOrLoseLife(
                state,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    discardedCreature = skippedDiscardedCreature
                ),
                priorEvents
            )
        }

        // If next player has exactly 1 card, auto-discard
        if (nextHand.size == 1) {
            val cardId = nextHand.first()
            val isCreature = state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true
            val graveyardZone = ZoneKey(nextPlayer, Zone.GRAVEYARD)
            var newState = state.removeFromZone(nextHandZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val autoDiscardEvents = priorEvents + listOf(CardsDiscardedEvent(nextPlayer, listOf(cardId)))
            val autoDiscardedCreature = continuation.discardedCreature + (nextPlayer to isCreature)

            return continueEachPlayerDiscardsOrLoseLife(
                newState,
                continuation.copy(
                    remainingPlayers = nextRemainingPlayers,
                    discardedCreature = autoDiscardedCreature
                ),
                autoDiscardEvents
            )
        }

        // Create decision for next player
        val decisionHandler = DecisionHandler()
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = nextPlayer,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            prompt = "Choose a card to discard",
            options = nextHand,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val newContinuation = continuation.copy(
            decisionId = decisionResult.pendingDecision!!.id,
            currentPlayerId = nextPlayer,
            remainingPlayers = nextRemainingPlayers
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            priorEvents + decisionResult.events
        )
    }

    /**
     * Resume after a player chose how many cards to draw for "each player may draw" effects.
     *
     * For effects like Temporary Truce where each player chooses 0-N cards to draw
     * and gains life for each card not drawn.
     */
    private fun resumeEachPlayerChoosesDraw(
        state: GameState,
        continuation: EachPlayerChoosesDrawContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response")
        }

        val chosenCount = response.number
        val currentPlayerId = continuation.currentPlayerId

        // Calculate life gain for this player
        val cardsNotDrawn = continuation.maxCards - chosenCount
        val lifeGain = cardsNotDrawn * continuation.lifePerCardNotDrawn

        // Update draw amounts and life gain amounts with this player's choice
        val newDrawAmounts = continuation.drawAmounts + (currentPlayerId to chosenCount)
        val newLifeGainAmounts = continuation.lifeGainAmounts + (currentPlayerId to lifeGain)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            // Ask next player
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            // Calculate actual max for next player (can't draw more than library size)
            val libraryZone = ZoneKey(nextPlayer, Zone.LIBRARY)
            val librarySize = state.getZone(libraryZone).size
            val actualMax = continuation.maxCards.coerceAtMost(librarySize)

            val prompt = if (continuation.lifePerCardNotDrawn > 0) {
                "Choose how many cards to draw (0-$actualMax). Gain ${continuation.lifePerCardNotDrawn} life for each card not drawn."
            } else {
                "Choose how many cards to draw (0-$actualMax)"
            }

            // Create decision for next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = prompt,
                minValue = 0,
                maxValue = actualMax,
                phase = DecisionPhase.RESOLUTION
            )

            // Push updated continuation
            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                drawAmounts = newDrawAmounts,
                lifeGainAmounts = newLifeGainAmounts
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        // All players have chosen - execute draws and life gains
        val result = EachPlayerMayDrawExecutor.executeDrawsAndLifeGains(
            state,
            newDrawAmounts,
            newLifeGainAmounts
        )

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after player selected cards to put in opponent's graveyard.
     *
     * This is the first step of LookAtOpponentLibraryEffect:
     * 1. Move selected cards to opponent's graveyard
     * 2. If there are remaining cards, ask player to reorder them
     * 3. Put remaining cards back on top of opponent's library in the chosen order
     */
    private fun resumeLookAtOpponentLibrary(
        state: GameState,
        continuation: LookAtOpponentLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for look at opponent library")
        }

        val selectedForGraveyard = response.selectedCards
        val opponentId = continuation.opponentId
        val playerId = continuation.playerId

        // Validate selection count
        if (selectedForGraveyard.size != continuation.toGraveyard) {
            return ExecutionResult.error(
                state,
                "Expected ${continuation.toGraveyard} cards for graveyard, got ${selectedForGraveyard.size}"
            )
        }

        val libraryZone = ZoneKey(opponentId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(opponentId, Zone.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to graveyard
        for (cardId in selectedForGraveyard) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.GRAVEYARD,
                    ownerId = opponentId
                )
            )
        }

        // Calculate remaining cards that need to be reordered
        val remainingCards = continuation.allCards.filter { it !in selectedForGraveyard }

        // If only 0 or 1 cards remain, no reordering needed
        if (remainingCards.size <= 1) {
            // Cards are already in the correct position at the top of the library
            // (they haven't been moved, just the graveyard cards were removed)
            return checkForMoreContinuations(newState, events)
        }

        // Need to reorder remaining cards - create a ReorderLibraryDecision
        val cardInfoMap = remainingCards.associateWith { cardId ->
            val container = newState.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Put the remaining ${remainingCards.size} cards on top of opponent's library in any order.",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = remainingCards,
            cardInfo = cardInfoMap
        )

        val reorderContinuation = ReorderOpponentLibraryContinuation(
            decisionId = decisionId,
            playerId = playerId,
            opponentId = opponentId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName
        )

        newState = newState
            .withPendingDecision(decision)
            .pushContinuation(reorderContinuation)

        events.add(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = playerId,
                decisionType = "REORDER_LIBRARY",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(newState, decision, events)
    }

    /**
     * Resume after player reordered the remaining cards for opponent's library.
     *
     * This is the second step of LookAtOpponentLibraryEffect:
     * Put the cards back on top of opponent's library in the chosen order.
     */
    private fun resumeReorderOpponentLibrary(
        state: GameState,
        continuation: ReorderOpponentLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for reorder opponent library")
        }

        val opponentId = continuation.opponentId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(opponentId, Zone.LIBRARY)

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they should be at the top)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards back on top in the new order
        val newLibrary = orderedCards + remainingLibrary

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            LibraryReorderedEvent(
                playerId = opponentId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player made a choice for a generic pay or suffer effect.
     *
     * Handles both card selection responses (for discard/sacrifice costs)
     * and yes/no responses (for random discard and pay life costs).
     */
    private fun resumePayOrSuffer(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val sourceName = continuation.sourceName

        return when (continuation.costType) {
            PayOrSufferCostType.DISCARD -> {
                if (continuation.random) {
                    resumePayOrSufferRandomDiscard(state, continuation, response)
                } else {
                    resumePayOrSufferDiscard(state, continuation, response)
                }
            }
            PayOrSufferCostType.SACRIFICE -> resumePayOrSufferSacrifice(state, continuation, response)
            PayOrSufferCostType.PAY_LIFE -> resumePayOrSufferPayLife(state, continuation, response)
        }
    }

    /**
     * Handle discard cost selection for pay or suffer.
     */
    private fun resumePayOrSufferDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer discard")
        }

        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player paid the cost - discard the selected cards
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in selectedCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.HAND,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        events.add(0, CardsDiscardedEvent(playerId, selectedCards))
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Handle random discard yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferRandomDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer random discard")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player chose to pay - execute random discard
        val result = com.wingedsheep.engine.handlers.effects.removal.PayOrSufferExecutor.executeRandomDiscard(
            state,
            continuation.playerId,
            continuation.filter,
            continuation.requiredCount
        )
        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Handle sacrifice cost selection for pay or suffer.
     */
    private fun resumePayOrSufferSacrifice(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // If player didn't select enough permanents, execute the suffer effect
        if (selectedPermanents.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player paid the cost - sacrifice the selected permanents
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in selectedPermanents) {
            val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)
            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = permanentName,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        events.add(0, PermanentsSacrificedEvent(playerId, selectedPermanents))
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Handle pay life yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferPayLife(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer pay life")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation)
        }

        // Player chose to pay life
        val playerId = continuation.playerId
        val lifeToPay = continuation.requiredCount
        val playerContainer = state.getEntity(playerId)
        val currentLife = playerContainer?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
        val newLife = currentLife - lifeToPay

        val newState = state.updateEntity(playerId) {
            it.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
        }

        val events = listOf(
            LifeChangedEvent(
                playerId = playerId,
                oldLife = currentLife,
                newLife = newLife,
                reason = LifeChangeReason.PAYMENT
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after a player decides whether to pay for "any player may [cost]" effects.
     */
    private fun resumeAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for any player may pay")
        }

        val selectedPermanents = response.selectedCards
        val playerId = continuation.currentPlayerId

        // If player selected enough permanents, they paid the cost
        if (selectedPermanents.size >= continuation.requiredCount) {
            // Sacrifice the selected permanents
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (permanentId in selectedPermanents) {
                val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
                newState = newState.removeFromZone(battlefieldZone, permanentId)
                newState = newState.addToZone(graveyardZone, permanentId)
                events.add(
                    ZoneChangeEvent(
                        entityId = permanentId,
                        entityName = permanentName,
                        fromZone = Zone.BATTLEFIELD,
                        toZone = Zone.GRAVEYARD,
                        ownerId = playerId
                    )
                )
            }
            events.add(0, PermanentsSacrificedEvent(playerId, selectedPermanents))

            // Now execute the consequence (e.g., sacrifice the source)
            val context = EffectContext(
                sourceId = continuation.sourceId,
                controllerId = continuation.controllerId,
                opponentId = null
            )
            val consequenceResult = effectExecutorRegistry.execute(newState, continuation.consequence, context)
            val allEvents = events + consequenceResult.events

            return if (consequenceResult.isPaused) {
                consequenceResult
            } else {
                checkForMoreContinuations(consequenceResult.state, allEvents)
            }
        }

        // Player declined - find next player who can pay
        return askNextPlayerForAnyPlayerMayPay(state, continuation)
    }

    /**
     * Find and ask the next eligible player for "any player may [cost]" effects.
     * If no player can pay, nothing happens and execution continues.
     */
    private fun askNextPlayerForAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation
    ): ExecutionResult {
        val predicateEvaluator = PredicateEvaluator()
        val cost = continuation.cost

        // Find next player who can pay among remaining players
        for ((index, nextPlayerId) in continuation.remainingPlayers.withIndex()) {
            if (cost is PayCost.Sacrifice) {
                val battlefieldZone = ZoneKey(nextPlayerId, Zone.BATTLEFIELD)
                val battlefield = state.getZone(battlefieldZone)
                val context = PredicateContext(controllerId = nextPlayerId)

                val validPermanents = battlefield.filter { permanentId ->
                    predicateEvaluator.matches(state, permanentId, cost.filter, context)
                }

                if (validPermanents.size >= cost.count) {
                    // This player can pay - ask them
                    val prompt = "You may sacrifice ${cost.count} ${cost.filter.description}s to cause ${continuation.sourceName} to be sacrificed, or skip"
                    val decisionHandler = DecisionHandler()

                    val decisionResult = decisionHandler.createCardSelectionDecision(
                        state = state,
                        playerId = nextPlayerId,
                        sourceId = continuation.sourceId,
                        sourceName = continuation.sourceName,
                        prompt = prompt,
                        options = validPermanents,
                        minSelections = 0,
                        maxSelections = cost.count,
                        ordered = false,
                        phase = DecisionPhase.RESOLUTION,
                        useTargetingUI = true
                    )

                    val newContinuation = continuation.copy(
                        decisionId = decisionResult.pendingDecision!!.id,
                        currentPlayerId = nextPlayerId,
                        remainingPlayers = continuation.remainingPlayers.drop(index + 1)
                    )

                    val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

                    return ExecutionResult.paused(
                        stateWithContinuation,
                        decisionResult.pendingDecision,
                        decisionResult.events
                    )
                }
            }
        }

        // No player can pay - nothing happens, Pangolin stays
        return checkForMoreContinuations(state, emptyList())
    }

    /**
     * Resume after player distributed damage among targets.
     */
    private fun resumeDistributeDamage(
        state: GameState,
        continuation: DistributeDamageContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for divided damage")
        }

        val distribution = response.distribution
        val events = mutableListOf<GameEvent>()
        var newState = state

        // Deal damage to each target according to the distribution
        for ((targetId, damageAmount) in distribution) {
            if (damageAmount > 0) {
                val result = EffectExecutorUtils.dealDamageToTarget(
                    newState,
                    targetId,
                    damageAmount,
                    continuation.sourceId
                )

                if (!result.isSuccess) {
                    return ExecutionResult(newState, events, result.error)
                }

                newState = result.state
                events.addAll(result.events)
            }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards to keep from looking at top cards of library.
     *
     * The player has selected which cards to put into their hand. The remaining
     * cards from the looked-at set go to graveyard (if restToGraveyard is true)
     * or stay on top of the library (if false).
     */
    private fun resumeLookAtTopCards(
        state: GameState,
        continuation: LookAtTopCardsContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for look at top cards")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards.toSet()
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Process all looked-at cards
        for (cardId in continuation.allCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // Remove from library first
            newState = newState.removeFromZone(libraryZone, cardId)

            if (cardId in selectedCards) {
                // Selected cards go to hand
                newState = newState.addToZone(handZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.HAND,
                        ownerId = playerId
                    )
                )
            } else {
                // Non-selected cards go to graveyard or stay on top of library
                if (continuation.restToGraveyard) {
                    newState = newState.addToZone(graveyardZone, cardId)
                    events.add(
                        ZoneChangeEvent(
                            entityId = cardId,
                            entityName = cardName,
                            fromZone = Zone.LIBRARY,
                            toZone = Zone.GRAVEYARD,
                            ownerId = playerId
                        )
                    )
                } else {
                    // Put back on top of library
                    val currentLibrary = newState.getZone(libraryZone)
                    newState = newState.copy(
                        zones = newState.zones + (libraryZone to listOf(cardId) + currentLibrary)
                    )
                }
            }
        }

        // Add a cards drawn event for the selected cards
        if (selectedCards.isNotEmpty()) {
            events.add(0, CardsDrawnEvent(playerId, selectedCards.size, selectedCards.toList()))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after opponent chose a card from revealed top cards.
     * Selected card goes to battlefield, rest go to graveyard.
     */
    private fun resumeRevealAndOpponentChooses(
        state: GameState,
        continuation: RevealAndOpponentChoosesContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for reveal and opponent chooses")
        }

        val controllerId = continuation.controllerId
        val selectedCard = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No card selected by opponent")

        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        for (cardId in continuation.allCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // Remove from library
            newState = newState.removeFromZone(libraryZone, cardId)

            if (cardId == selectedCard) {
                // Selected card goes to battlefield
                newState = newState.addToZone(battlefieldZone, cardId)

                // Apply battlefield components (controller, summoning sickness)
                val container = newState.getEntity(cardId)
                if (container != null) {
                    var newContainer = container
                        .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(controllerId))

                    val cardComponent = container.get<CardComponent>()
                    if (cardComponent?.typeLine?.isCreature == true) {
                        newContainer = newContainer.with(
                            com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                        )
                    }

                    newState = newState.copy(
                        entities = newState.entities + (cardId to newContainer)
                    )
                }

                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.BATTLEFIELD,
                        ownerId = controllerId
                    )
                )
            } else {
                // Non-selected cards go to graveyard
                newState = newState.addToZone(graveyardZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.GRAVEYARD,
                        ownerId = controllerId
                    )
                )
            }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chooses a color for protection granting effects.
     *
     * Creates floating effects granting protection from the chosen color to all
     * creatures matching the filter that the controller controls.
     */
    private fun resumeChooseColorProtection(
        state: GameState,
        continuation: ChooseColorProtectionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for protection effect")
        }

        val chosenColor = response.color
        val controllerId = continuation.controllerId
        val events = mutableListOf<GameEvent>()
        val affectedEntities = mutableSetOf<com.wingedsheep.sdk.model.EntityId>()
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = continuation.sourceId)
        val projected = StateProjector().project(state)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check excludeSelf
            if (continuation.filter.excludeSelf && entityId == continuation.sourceId) continue

            // Use projected state for correct face-down creature handling (Rule 707.2)
            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, continuation.filter.baseFilter, predicateContext)) {
                continue
            }

            affectedEntities.add(entityId)

            // Use projected name for face-down creatures (they have no name)
            val displayName = if (container.has<FaceDownComponent>()) "Face-down creature" else cardComponent.name
            events.add(
                KeywordGrantedEvent(
                    targetId = entityId,
                    targetName = displayName,
                    keyword = "Protection from ${chosenColor.displayName.lowercase()}",
                    sourceName = continuation.sourceName ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return checkForMoreContinuations(state, events)
        }

        val floatingEffect = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = com.wingedsheep.sdk.model.EntityId.generate(),
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.ABILITY,
                sublayer = null,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.GrantProtectionFromColor(
                    chosenColor.name
                ),
                affectedEntities = affectedEntities
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chooses a color for single-target protection granting effects.
     *
     * Creates a floating effect granting protection from the chosen color to the
     * specific target entity.
     */
    private fun resumeChooseColorProtectionTarget(
        state: GameState,
        continuation: ChooseColorProtectionTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for protection effect")
        }

        val chosenColor = response.color
        val targetEntityId = continuation.targetEntityId
        val events = mutableListOf<GameEvent>()

        // Verify the target is still on the battlefield
        val container = state.getEntity(targetEntityId)
        val cardComponent = container?.get<CardComponent>()
        if (container == null || cardComponent == null || !state.getBattlefield().contains(targetEntityId)) {
            return checkForMoreContinuations(state, events)
        }

        val displayName = if (container.has<FaceDownComponent>()) "Face-down creature" else cardComponent.name
        events.add(
            KeywordGrantedEvent(
                targetId = targetEntityId,
                targetName = displayName,
                keyword = "Protection from ${chosenColor.displayName.lowercase()}",
                sourceName = continuation.sourceName ?: "Unknown"
            )
        )

        val floatingEffect = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = com.wingedsheep.sdk.model.EntityId.generate(),
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.ABILITY,
                sublayer = null,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.GrantProtectionFromColor(
                    chosenColor.name
                ),
                affectedEntities = setOf(targetEntityId)
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for graveyard retrieval.
     * Filters graveyard for creatures of that type and presents a card selection.
     */
    private fun resumeChooseCreatureTypeReturn(
        state: GameState,
        continuation: ChooseCreatureTypeReturnContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val controllerId = continuation.controllerId
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val graveyard = state.getZone(graveyardZone)

        // Find creature cards of the chosen type in the graveyard
        val matchingCards = graveyard.filter { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            val typeLine = cardComponent?.typeLine
            typeLine != null && typeLine.isCreature && typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))
        }

        // If no matching cards, nothing to return
        if (matchingCards.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        // Build card info for the UI
        val cardInfoMap = matchingCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = cardComponent?.imageUri
            )
        }

        val actualMax = minOf(continuation.count, matchingCards.size)
        val decisionId = java.util.UUID.randomUUID().toString()

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose up to $actualMax ${chosenType} card${if (actualMax != 1) "s" else ""} to return to your hand",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 0,
            maxSelections = actualMax,
            ordered = false,
            cardInfo = cardInfoMap
        )

        val nextContinuation = GraveyardToHandContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(nextContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after player selected cards from graveyard to return to hand.
     */
    private fun resumeGraveyardToHand(
        state: GameState,
        continuation: GraveyardToHandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for graveyard to hand")
        }

        val controllerId = continuation.controllerId
        val selectedCards = response.selectedCards
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val handZone = ZoneKey(controllerId, Zone.HAND)
        val events = mutableListOf<GameEvent>()

        var newState = state

        for (cardId in selectedCards) {
            // Validate card is still in graveyard
            if (cardId !in newState.getZone(graveyardZone)) continue

            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(graveyardZone, cardId)
            newState = newState.addToZone(handZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.GRAVEYARD,
                    toZone = Zone.HAND,
                    ownerId = controllerId
                )
            )
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chooses the FROM creature type for text replacement.
     * Presents the TO creature type choice (excluding Wall).
     */
    private fun resumeChooseFromCreatureType(
        state: GameState,
        continuation: ChooseFromCreatureTypeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val fromType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Present TO creature type choice, excluding any types specified by the effect
        val excludedTypes = continuation.excludedTypes.map { it.lowercase() }.toSet()
        val toOptions = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES.filter {
            it.lowercase() !in excludedTypes
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val promptSuffix = if (continuation.excludedTypes.isNotEmpty()) {
            ", can't be ${continuation.excludedTypes.joinToString(" or ")}"
        } else ""
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Choose the replacement creature type (replacing $fromType$promptSuffix)",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = toOptions
        )

        val nextContinuation = ChooseToCreatureTypeContinuation(
            decisionId = decisionId,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            targetId = continuation.targetId,
            fromType = fromType,
            creatureTypes = toOptions
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(nextContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after player chooses the TO creature type for text replacement.
     * Applies the TextReplacementComponent to the target entity.
     */
    private fun resumeChooseToCreatureType(
        state: GameState,
        continuation: ChooseToCreatureTypeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val toType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still exist
        if (state.getEntity(targetId) == null) {
            return checkForMoreContinuations(state, emptyList())
        }

        // Add or update TextReplacementComponent on the target
        val existingComponent = state.getEntity(targetId)
            ?.get<com.wingedsheep.engine.state.components.identity.TextReplacementComponent>()

        val replacement = com.wingedsheep.engine.state.components.identity.TextReplacement(
            fromWord = continuation.fromType,
            toWord = toType,
            category = com.wingedsheep.engine.state.components.identity.TextReplacementCategory.CREATURE_TYPE
        )

        val newComponent = existingComponent?.withReplacement(replacement)
            ?: com.wingedsheep.engine.state.components.identity.TextReplacementComponent(
                replacements = listOf(replacement)
            )

        val newState = state.updateEntity(targetId) { container ->
            container.with(newComponent)
        }

        return checkForMoreContinuations(newState, emptyList())
    }

    /**
     * Execute the suffer effect for pay or suffer.
     */
    private fun executePayOrSufferConsequence(
        state: GameState,
        continuation: PayOrSufferContinuation
    ): ExecutionResult {
        val sourceId = continuation.sourceId
        val playerId = continuation.playerId
        val sufferEffect = continuation.sufferEffect

        // Create context for executing the suffer effect, preserving targets from the original trigger
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = playerId,
            opponentId = null,
            targets = continuation.targets
        )

        // Execute the suffer effect using the registry
        val result = effectExecutorRegistry.execute(state, sufferEffect, context)

        return if (result.isPaused) {
            result
        } else {
            checkForMoreContinuations(result.state, result.events.toList())
        }
    }

    /**
     * Resume after target player revealed cards for Blackmail.
     * Now the controller must choose one of the revealed cards for the target to discard.
     */
    private fun resumeBlackmailReveal(
        state: GameState,
        continuation: BlackmailRevealContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Blackmail reveal")
        }

        val revealedCards = response.selectedCards

        // Now ask the controller to choose one of the revealed cards
        return BlackmailExecutor.askControllerToChoose(
            state = state,
            controllerId = continuation.controllerId,
            targetPlayerId = continuation.targetPlayerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            revealedCards = revealedCards
        )
    }

    /**
     * Resume after controller chose a card from Blackmail reveal.
     * Discard the chosen card from the target player's hand.
     */
    private fun resumeBlackmailChoose(
        state: GameState,
        continuation: BlackmailChooseContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Blackmail choose")
        }

        val selectedCards = response.selectedCards
        if (selectedCards.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardToDiscard = selectedCards.first()
        val targetPlayerId = continuation.targetPlayerId
        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val graveyardZone = ZoneKey(targetPlayerId, Zone.GRAVEYARD)

        var newState = state
        newState = newState.removeFromZone(handZone, cardToDiscard)
        newState = newState.addToZone(graveyardZone, cardToDiscard)

        val events = listOf(
            CardsDiscardedEvent(targetPlayerId, listOf(cardToDiscard))
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after controller selected cards from opponent's library for Head Games.
     * Move selected cards to opponent's hand, then shuffle opponent's library.
     */
    private fun resumeHeadGames(
        state: GameState,
        continuation: HeadGamesContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Head Games")
        }

        val targetPlayerId = continuation.targetPlayerId
        val libraryZone = ZoneKey(targetPlayerId, Zone.LIBRARY)
        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val selectedCards = response.selectedCards
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to opponent's hand
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)

            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardComponent?.name ?: "Unknown",
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.HAND,
                    ownerId = targetPlayerId
                )
            )
        }

        // Shuffle opponent's library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))
        events.add(LibraryShuffledEvent(targetPlayerId))

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for "reveal top card" effects.
     *
     * Reveals the top card. If it's a creature of the chosen type, put it into hand.
     * Otherwise, put it into graveyard.
     */
    private fun resumeChooseCreatureTypeRevealTop(
        state: GameState,
        continuation: ChooseCreatureTypeRevealTopContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val controllerId = continuation.controllerId
        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // If library became empty since the ability was activated, nothing happens
        if (library.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val topCardId = library.first()
        val cardComponent = state.getEntity(topCardId)?.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"
        val cardImageUri = cardComponent?.imageUri

        // Reveal the card
        val revealEvent = CardsRevealedEvent(
            revealingPlayerId = controllerId,
            cardIds = listOf(topCardId),
            cardNames = listOf(cardName),
            imageUris = listOf(cardImageUri),
            source = continuation.sourceName
        )

        // Check if the card is a creature of the chosen type
        val typeLine = cardComponent?.typeLine
        val isMatch = typeLine != null &&
            typeLine.isCreature &&
            typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))

        var newState = state.removeFromZone(libraryZone, topCardId)
        val events = mutableListOf<GameEvent>(revealEvent)

        if (isMatch) {
            // Put into hand
            val handZone = ZoneKey(controllerId, Zone.HAND)
            newState = newState.addToZone(handZone, topCardId)
            events.add(
                ZoneChangeEvent(
                    entityId = topCardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.HAND,
                    ownerId = controllerId
                )
            )
        } else {
            // Put into graveyard
            val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
            newState = newState.addToZone(graveyardZone, topCardId)
            events.add(
                ZoneChangeEvent(
                    entityId = topCardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.GRAVEYARD,
                    ownerId = controllerId
                )
            )
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for "reveal until creature type" effects.
     *
     * Reveals cards from the top of the library until a creature of the chosen type is found.
     * Puts that creature onto the battlefield and shuffles the rest into the library.
     */
    private fun resumeRevealUntilCreatureType(
        state: GameState,
        continuation: RevealUntilCreatureTypeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val controllerId = continuation.controllerId
        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // If library became empty since the ability was activated, nothing happens
        if (library.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        // Reveal cards from the top until we find a creature of the chosen type
        val revealedCards = mutableListOf<com.wingedsheep.sdk.model.EntityId>()
        var matchedCard: com.wingedsheep.sdk.model.EntityId? = null

        for (cardId in library) {
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            revealedCards.add(cardId)

            val typeLine = cardComponent?.typeLine
            if (typeLine != null &&
                typeLine.isCreature &&
                typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))
            ) {
                matchedCard = cardId
                break
            }
        }

        val events = mutableListOf<GameEvent>()

        // Emit reveal event for all revealed cards
        val cardNames = revealedCards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        }
        val imageUris = revealedCards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.imageUri
        }

        events.add(
            CardsRevealedEvent(
                revealingPlayerId = controllerId,
                cardIds = revealedCards.toList(),
                cardNames = cardNames,
                imageUris = imageUris,
                source = continuation.sourceName
            )
        )

        var newState = state

        if (matchedCard != null) {
            // Remove matched card from library and put onto battlefield
            newState = newState.removeFromZone(libraryZone, matchedCard)
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, matchedCard)

            // Apply battlefield components
            val container = newState.getEntity(matchedCard)
            if (container != null) {
                var newContainer = container
                    .with(ControllerComponent(controllerId))

                val cardComponent = container.get<CardComponent>()
                if (cardComponent?.typeLine?.isCreature == true) {
                    newContainer = newContainer.with(
                        com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                    )
                }

                newState = newState.copy(
                    entities = newState.entities + (matchedCard to newContainer)
                )
            }

            val matchedCardName = state.getEntity(matchedCard)?.get<CardComponent>()?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = matchedCard,
                    entityName = matchedCardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )

            // Shuffle the rest of the revealed cards back into the library
            // (they are still in the library since we only removed the matched card)
            // The other revealed cards are still in the library at their positions -
            // we just need to shuffle the entire library
            val currentLibrary = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(
                zones = newState.zones + (libraryZone to currentLibrary)
            )
            events.add(LibraryShuffledEvent(controllerId))
        } else {
            // No creature of the chosen type found - shuffle library (all cards stay)
            val currentLibrary = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(
                zones = newState.zones + (libraryZone to currentLibrary)
            )
            events.add(LibraryShuffledEvent(controllerId))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for a "becomes the creature type
     * of your choice" effect. Creates a floating effect that replaces all creature
     * subtypes with the chosen type.
     */
    private fun resumeBecomeCreatureType(
        state: GameState,
        continuation: BecomeCreatureTypeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still be on the battlefield
        if (targetId !in state.getBattlefield()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
        val targetName = targetCard?.name ?: "creature"

        // Create a floating effect that sets creature subtypes
        val floatingEffect = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.TYPE,
                sublayer = null,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.SetCreatureSubtypes(
                    subtypes = setOf(chosenType)
                ),
                affectedEntities = setOf(targetId)
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        val events = listOf(
            CreatureTypeChangedEvent(
                targetId = targetId,
                targetName = targetName,
                newType = chosenType,
                sourceName = continuation.sourceName ?: "Unknown"
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for stat modification.
     *
     * Creates a floating effect that modifies P/T for all creatures of the chosen type.
     */
    private fun resumeChooseCreatureTypeModifyStats(
        state: GameState,
        continuation: ChooseCreatureTypeModifyStatsContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Find all creatures of the chosen type on the battlefield
        // Use projected state to check subtypes, so type-changing continuous effects
        // (e.g., Mistform Dreamer becoming a Cleric) are taken into account
        val projected = StateProjector().project(state)
        val affectedEntities = mutableSetOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if creature (face-down permanents are always creatures per Rule 707.2)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            // Check if creature has the chosen subtype using projected state
            if (!projected.hasSubtype(entityId, chosenType)) continue

            affectedEntities.add(entityId)
            events.add(
                StatsModifiedEvent(
                    targetId = entityId,
                    targetName = cardComponent.name,
                    powerChange = continuation.powerModifier,
                    toughnessChange = continuation.toughnessModifier,
                    sourceName = continuation.sourceName ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val floatingEffect = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.POWER_TOUGHNESS,
                sublayer = com.wingedsheep.engine.mechanics.layers.Sublayer.MODIFICATIONS,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.ModifyPowerToughness(
                    powerMod = continuation.powerModifier,
                    toughnessMod = continuation.toughnessModifier
                ),
                affectedEntities = affectedEntities
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for global type change.
     *
     * Sets all creatures on the battlefield to the chosen type via a floating effect.
     */
    private fun resumeBecomeChosenTypeAllCreatures(
        state: GameState,
        continuation: BecomeChosenTypeAllCreaturesContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Find all creatures on the battlefield
        val affectedEntities = mutableSetOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if creature (face-down permanents are always creatures per Rule 707.2)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            affectedEntities.add(entityId)
            events.add(
                CreatureTypeChangedEvent(
                    targetId = entityId,
                    targetName = cardComponent.name,
                    newType = chosenType,
                    sourceName = continuation.sourceName ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val floatingEffect = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.TYPE,
                sublayer = null,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.SetCreatureSubtypes(
                    subtypes = setOf(chosenType)
                ),
                affectedEntities = affectedEntities
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a mode for a modal spell.
     *
     * After mode selection:
     * - If the chosen mode has target requirements, pause for target selection.
     * - If the chosen mode has no targets, execute the effect directly.
     */
    private fun resumeModal(
        state: GameState,
        continuation: ModalContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for modal spell")
        }

        val modeIndex = response.optionIndex
        if (modeIndex < 0 || modeIndex >= continuation.modes.size) {
            return ExecutionResult.error(state, "Invalid mode index: $modeIndex")
        }

        val chosenMode = continuation.modes[modeIndex]

        // If the chosen mode has target requirements, pause for target selection
        if (chosenMode.targetRequirements.isNotEmpty()) {
            val sourceId = continuation.sourceId
            val sourceName = continuation.sourceName ?: "modal spell"

            // Find valid targets for each requirement
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = chosenMode.targetRequirements.mapIndexed { index, req ->
                val legalTargets = targetFinder.findLegalTargets(
                    state = state,
                    requirement = req,
                    controllerId = continuation.controllerId,
                    sourceId = sourceId
                )
                legalTargetsMap[index] = legalTargets
                TargetRequirementInfo(
                    index = index,
                    description = req.description,
                    minTargets = req.effectiveMinCount,
                    maxTargets = req.count
                )
            }

            // Check if all requirements can be satisfied
            val allSatisfied = requirementInfos.all { info ->
                (legalTargetsMap[info.index]?.isNotEmpty() == true) || info.minTargets == 0
            }

            if (!allSatisfied) {
                // No valid targets for the chosen mode - fizzle
                return checkForMoreContinuations(state, emptyList())
            }

            // If single player-target requirement with exactly one valid target, auto-select
            if (chosenMode.targetRequirements.size == 1) {
                val req = chosenMode.targetRequirements[0]
                val targets = legalTargetsMap[0] ?: emptyList()
                val isPlayerTarget = req is TargetPlayer || req is TargetOpponent
                if (isPlayerTarget && targets.size == 1 && req.count == 1) {
                    // Auto-select the single target
                    val chosenTarget = entityIdToChosenTarget(state, targets[0])
                    val context = EffectContext(
                        sourceId = sourceId,
                        controllerId = continuation.controllerId,
                        opponentId = continuation.opponentId,
                        xValue = continuation.xValue,
                        targets = listOf(chosenTarget)
                    )
                    val result = effectExecutorRegistry.execute(state, chosenMode.effect, context)
                    if (result.isPaused) return result
                    return checkForMoreContinuations(result.state, result.events.toList())
                }
            }

            // Create target selection decision
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = continuation.controllerId,
                prompt = "Choose targets for $sourceName",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                targetRequirements = requirementInfos,
                legalTargets = legalTargetsMap
            )

            val modalTargetContinuation = ModalTargetContinuation(
                decisionId = decisionId,
                controllerId = continuation.controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                effect = chosenMode.effect,
                xValue = continuation.xValue,
                opponentId = continuation.opponentId
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(modalTargetContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = continuation.controllerId,
                        decisionType = "CHOOSE_TARGETS",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // No targets needed - execute the effect directly
        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = continuation.opponentId,
            xValue = continuation.xValue
        )

        val result = effectExecutorRegistry.execute(state, chosenMode.effect, context)
        if (result.isPaused) return result
        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after player selected targets for a modal spell mode.
     * Execute the chosen mode's effect with the selected targets.
     */
    private fun resumeModalTarget(
        state: GameState,
        continuation: ModalTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for modal spell")
        }

        // Convert selected targets to ChosenTargets
        val chosenTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { targetId ->
                entityIdToChosenTarget(state, targetId)
            }
        }

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = continuation.opponentId,
            xValue = continuation.xValue,
            targets = chosenTargets
        )

        val result = effectExecutorRegistry.execute(state, continuation.effect, context)
        if (result.isPaused) return result
        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Resume after player selects a creature to copy for Clone-style effects.
     */
    private fun resumeCloneEnters(
        state: GameState,
        continuation: CloneEntersContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for clone")
        }

        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId
        val events = mutableListOf<GameEvent>()

        val spellContainer = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Clone spell entity not found: $spellId")

        val originalCardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Clone spell has no CardComponent")

        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Clone spell has no SpellOnStackComponent")

        var newState = state

        // If a creature was selected, copy its CardComponent
        val selectedCreatureId = response.selectedCards.firstOrNull()
        val copiedCardDef: com.wingedsheep.sdk.model.CardDefinition?

        if (selectedCreatureId != null) {
            val targetContainer = newState.getEntity(selectedCreatureId)
            val targetCardComponent = targetContainer?.get<CardComponent>()

            if (targetCardComponent != null) {
                // Create a copy of the target's CardComponent, keeping Clone's ownerId
                val copiedCardComponent = targetCardComponent.copy(
                    ownerId = ownerId
                )

                // Update entity with copied card component and copy tracking
                newState = newState.updateEntity(spellId) { c ->
                    c.with(copiedCardComponent)
                        .with(com.wingedsheep.engine.state.components.identity.CopyOfComponent(
                            originalCardDefinitionId = originalCardComponent.cardDefinitionId,
                            copiedCardDefinitionId = targetCardComponent.cardDefinitionId
                        ))
                }

                // Look up the card definition for the copied creature
                copiedCardDef = stackResolver.cardRegistry?.getCard(targetCardComponent.cardDefinitionId)
            } else {
                // Target creature no longer exists - enter as itself
                copiedCardDef = stackResolver.cardRegistry?.getCard(originalCardComponent.cardDefinitionId)
            }
        } else {
            // Player declined to copy - enter as itself (0/0 Clone)
            copiedCardDef = stackResolver.cardRegistry?.getCard(originalCardComponent.cardDefinitionId)
        }

        // Get the (possibly updated) card component for event names
        val finalCardComponent = newState.getEntity(spellId)?.get<CardComponent>() ?: originalCardComponent

        // Complete the permanent entry using the shared helper
        newState = stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, finalCardComponent, copiedCardDef
        )

        events.add(ResolvedEvent(spellId, finalCardComponent.name))
        events.add(
            ZoneChangeEvent(
                spellId,
                finalCardComponent.name,
                null,
                Zone.BATTLEFIELD,
                ownerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chooses a color for an "as enters, choose a color" effect (e.g., Riptide Replicator).
     * After storing the color, checks if the card also has EntersWithCreatureTypeChoice and chains to that.
     */
    private fun resumeChooseColorEnters(
        state: GameState,
        continuation: ChooseColorEntersContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for color enters effect")
        }

        val chosenColor = response.color
        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId

        // Store the chosen color on the entity
        var newState = state.updateEntity(spellId) { c ->
            c.with(com.wingedsheep.engine.state.components.identity.ChosenColorComponent(chosenColor))
        }

        val spellContainer = newState.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")

        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")

        // Check if the card also needs a creature type choice
        val cardDef = stackResolver.cardRegistry?.getCard(cardComponent.cardDefinitionId)
        val entersWithCreatureTypeChoice = cardDef?.script?.replacementEffects
            ?.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice>()?.firstOrNull()

        if (entersWithCreatureTypeChoice != null) {
            // Chain to creature type choice
            val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
            val chooserId = if (entersWithCreatureTypeChoice.opponentChooses) {
                newState.turnOrder.firstOrNull { it != controllerId } ?: controllerId
            } else {
                controllerId
            }
            val decisionId = "choose-creature-type-enters-${spellId.value}"
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = chooserId,
                prompt = "Choose a creature type",
                context = DecisionContext(
                    sourceId = spellId,
                    sourceName = cardComponent.name,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = allCreatureTypes,
                defaultSearch = ""
            )

            val creatureTypeContinuation = ChooseCreatureTypeEntersContinuation(
                decisionId = decisionId,
                spellId = spellId,
                controllerId = controllerId,
                ownerId = ownerId,
                creatureTypes = allCreatureTypes
            )

            val pausedState = newState
                .pushContinuation(creatureTypeContinuation)
                .withPendingDecision(decision)
            return ExecutionResult.paused(pausedState, decision)
        }

        // No creature type choice needed - complete the permanent entry
        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        newState = stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, cardComponent, cardDef
        )

        val events = mutableListOf<GameEvent>()
        events.add(ResolvedEvent(spellId, cardComponent.name))
        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent.name,
                null,
                Zone.BATTLEFIELD,
                ownerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chooses a creature type for an "as enters" effect (e.g., Doom Cannon).
     */
    private fun resumeChooseCreatureTypeEnters(
        state: GameState,
        continuation: ChooseCreatureTypeEntersContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option chosen response for creature type choice")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId
        val events = mutableListOf<GameEvent>()

        val spellContainer = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")

        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")

        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        // Store the chosen creature type on the entity
        var newState = state.updateEntity(spellId) { c ->
            c.with(com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent(chosenType))
        }

        // Complete the permanent entry
        val cardDef = stackResolver.cardRegistry?.getCard(cardComponent.cardDefinitionId)
        newState = stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, cardComponent, cardDef
        )

        events.add(ResolvedEvent(spellId, cardComponent.name))
        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent.name,
                null,
                Zone.BATTLEFIELD,
                ownerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume casting a spell after the player chose a creature type during casting.
     *
     * Completes the casting by putting the spell on the stack with the chosen type,
     * then detects and processes triggers (same as CastSpellHandler does).
     */
    private fun resumeCastWithCreatureType(
        state: GameState,
        continuation: CastWithCreatureTypeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Complete casting: put spell on stack with the chosen creature type
        val castResult = stackResolver.castSpell(
            state,
            continuation.cardId,
            continuation.casterId,
            continuation.targets,
            continuation.xValue,
            continuation.sacrificedPermanents,
            targetRequirements = continuation.targetRequirements,
            chosenCreatureType = chosenType
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var allEvents = castResult.events

        // Detect and process triggers from casting (same as CastSpellHandler does)
        if (triggerDetector != null && triggerProcessor != null) {
            val triggers = triggerDetector.detectTriggers(castResult.newState, allEvents)
            if (triggers.isNotEmpty()) {
                val triggerResult = triggerProcessor.processTriggers(castResult.newState, triggers)

                if (triggerResult.isPaused) {
                    return ExecutionResult.paused(
                        triggerResult.state.withPriority(continuation.casterId),
                        triggerResult.pendingDecision!!,
                        allEvents + triggerResult.events
                    )
                }

                allEvents = allEvents + triggerResult.events
                return ExecutionResult.success(
                    triggerResult.newState.withPriority(continuation.casterId),
                    allEvents
                )
            }
        }

        return ExecutionResult.success(
            castResult.newState.withPriority(continuation.casterId),
            allEvents
        )
    }

    /**
     * Resume after a player chose a number for secret bidding (Menacing Ogre).
     */
    private fun resumeSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response")
        }

        val chosenNumber = response.number
        val currentPlayerId = continuation.currentPlayerId

        val newChosenNumbers = continuation.chosenNumbers + (currentPlayerId to chosenNumber)

        // Check if there are more players
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemainingPlayers = continuation.remainingPlayers.drop(1)

            val prompt = "Secretly choose a number (you will lose that much life if you have the highest bid)"

            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                prompt = prompt,
                minValue = 0,
                maxValue = 99,
                phase = DecisionPhase.RESOLUTION
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemainingPlayers,
                chosenNumbers = newChosenNumbers
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        // All players have chosen - resolve the bid
        return resolveSecretBid(state, continuation, newChosenNumbers)
    }

    /**
     * Resolve the secret bid: find highest number, apply life loss and counters.
     */
    private fun resolveSecretBid(
        state: GameState,
        continuation: SecretBidContinuation,
        chosenNumbers: Map<EntityId, Int>
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        val highestBid = chosenNumbers.values.maxOrNull() ?: 0

        // Each player with the highest number loses that much life
        val highestBidders = chosenNumbers.filter { it.value == highestBid && it.value > 0 }

        for ((playerId, amount) in highestBidders) {
            val currentLife = currentState.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
            val newLife = currentLife - amount

            currentState = currentState.updateEntity(playerId) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
            }

            allEvents.add(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        }

        // If the controller is one of the highest bidders, put counters on the source
        if (highestBidders.containsKey(continuation.controllerId) && continuation.sourceId != null) {
            val counterType = try {
                com.wingedsheep.sdk.core.CounterType.valueOf(
                    continuation.counterType.uppercase()
                        .replace(' ', '_')
                        .replace('+', 'P')
                        .replace('-', 'M')
                        .replace("/", "_")
                )
            } catch (e: IllegalArgumentException) {
                com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            }

            val current = currentState.getEntity(continuation.sourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()

            currentState = currentState.updateEntity(continuation.sourceId) { container ->
                container.with(current.withAdded(counterType, continuation.counterCount))
            }

            allEvents.add(CountersAddedEvent(continuation.sourceId, continuation.counterType, continuation.counterCount))
        }

        return checkForMoreContinuations(currentState, allEvents)
    }

    /**
     * Check if there are more continuation frames to process.
     * If there's an EffectContinuation waiting, process it.
     */
    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val nextContinuation = state.peekContinuation()

        if (nextContinuation is PendingTriggersContinuation && triggerProcessor != null) {
            // Pop and process the remaining triggers
            val (_, stateAfterPop) = state.popContinuation()
            val triggerResult = triggerProcessor.processTriggers(stateAfterPop, nextContinuation.remainingTriggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            if (!triggerResult.isSuccess) {
                return ExecutionResult(
                    state = triggerResult.state,
                    events = events + triggerResult.events,
                    error = triggerResult.error
                )
            }

            return ExecutionResult.success(triggerResult.newState, events + triggerResult.events)
        }

        if (nextContinuation is EffectContinuation && nextContinuation.remainingEffects.isNotEmpty()) {
            // Pop and process the effect continuation
            val (_, stateAfterPop) = state.popContinuation()
            val context = nextContinuation.toEffectContext()
            var currentState = stateAfterPop
            val allEvents = events.toMutableList()

            for ((index, effect) in nextContinuation.remainingEffects.withIndex()) {
                val stillRemaining = nextContinuation.remainingEffects.drop(index + 1)

                // Pre-push EffectContinuation for remaining effects BEFORE executing.
                // This ensures that if the sub-effect pushes its own continuation,
                // that continuation ends up on TOP (to be processed first when the response comes).
                val stateForExecution = if (stillRemaining.isNotEmpty()) {
                    val remainingContinuation = EffectContinuation(
                        decisionId = "pending", // Will be found by checkForMoreContinuations
                        remainingEffects = stillRemaining,
                        sourceId = nextContinuation.sourceId,
                        controllerId = nextContinuation.controllerId,
                        opponentId = nextContinuation.opponentId,
                        xValue = nextContinuation.xValue
                    )
                    currentState.pushContinuation(remainingContinuation)
                } else {
                    currentState
                }

                val result = effectExecutorRegistry.execute(stateForExecution, effect, context)

                if (!result.isSuccess && !result.isPaused) {
                    // Sub-effect failed - skip it and continue with remaining effects.
                    // Per MTG rules, do as much as possible.
                    currentState = if (stillRemaining.isNotEmpty()) {
                        val (_, stateWithoutCont) = result.state.popContinuation()
                        stateWithoutCont
                    } else {
                        result.state
                    }
                    allEvents.addAll(result.events)
                    continue
                }

                if (result.isPaused) {
                    // Sub-effect paused. Its continuation is on top.
                    // Our pre-pushed EffectContinuation is underneath, ready to be
                    // processed by checkForMoreContinuations after the sub-effect resolves.
                    return ExecutionResult.paused(
                        result.state,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                // Effect succeeded - pop our pre-pushed continuation (it wasn't needed)
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        return ExecutionResult.success(state, events)
    }

    /**
     * Resume putting a card from hand onto the battlefield after card selection.
     */
    private fun resumePutFromHand(
        state: GameState,
        continuation: PutFromHandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for put-from-hand")
        }

        // Player selected 0 cards — declined
        if (response.selectedCards.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val cardId = response.selectedCards.first()
        val playerId = continuation.playerId
        val handZone = ZoneKey(playerId, Zone.HAND)

        // Verify card is still in hand
        if (cardId !in state.getZone(handZone)) {
            return checkForMoreContinuations(state, emptyList())
        }

        var newState = state

        // Remove from hand
        newState = newState.removeFromZone(handZone, cardId)

        // Add to battlefield
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, cardId)

        // Apply battlefield components
        val container = newState.getEntity(cardId)
        if (container != null) {
            var newContainer = container
                .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(playerId))

            // Creatures enter with summoning sickness
            val cardComponent = container.get<CardComponent>()
            if (cardComponent?.typeLine?.isCreature == true) {
                newContainer = newContainer.with(
                    com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                )
            }

            // Apply tapped status if entersTapped
            if (continuation.entersTapped) {
                newContainer = newContainer.with(TappedComponent)
            }

            newState = newState.copy(
                entities = newState.entities + (cardId to newContainer)
            )
        }

        // Emit zone change event
        val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"
        val events = listOf(
            ZoneChangeEvent(
                entityId = cardId,
                entityName = cardName,
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = playerId
            )
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected which permanents to keep tapped during untap step.
     *
     * Selected cards are permanents the player chose to keep tapped.
     * Everything else in allPermanentsToUntap gets untapped.
     */
    private fun resumeUntapChoice(
        state: GameState,
        continuation: UntapChoiceContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for untap choice")
        }

        val keepTapped = response.selectedCards.toSet()
        val toUntap = continuation.allPermanentsToUntap.filter { it !in keepTapped }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Untap the permanents that the player did NOT choose to keep tapped
        for (entityId in toUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardName))
        }

        // Remove WhileSourceTapped floating effects whose source is no longer tapped
        newState = newState.copy(
            floatingEffects = newState.floatingEffects.filter { floatingEffect ->
                if (floatingEffect.duration is Duration.WhileSourceTapped) {
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId) &&
                        newState.getEntity(sourceId)?.has<TappedComponent>() == true
                } else {
                    true
                }
            }
        )

        // Remove summoning sickness from all creatures the active player controls
        val activePlayer = continuation.playerId
        val projected = com.wingedsheep.engine.mechanics.layers.StateProjector().project(newState)
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projected.getController(entityId) == activePlayer &&
                container.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Convert an EntityId to the appropriate ChosenTarget type based on where it is in the game.
     */
    private fun entityIdToChosenTarget(state: GameState, entityId: EntityId): ChosenTarget {
        return when {
            entityId in state.turnOrder -> ChosenTarget.Player(entityId)
            entityId in state.getBattlefield() -> ChosenTarget.Permanent(entityId)
            entityId in state.stack -> ChosenTarget.Spell(entityId)
            else -> {
                val graveyardOwner = state.turnOrder.find { playerId ->
                    val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                    entityId in state.getZone(graveyardZone)
                }
                if (graveyardOwner != null) {
                    ChosenTarget.Card(entityId, graveyardOwner, Zone.GRAVEYARD)
                } else {
                    ChosenTarget.Permanent(entityId)
                }
            }
        }
    }

    /**
     * Resume after an opponent selected cards from their hand to put onto the battlefield.
     *
     * Moves selected cards from the opponent's hand to the battlefield, then asks the next
     * opponent (if any).
     */
    private fun resumeEachOpponentMayPutFromHand(
        state: GameState,
        continuation: EachOpponentMayPutFromHandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-opponent-may-put-from-hand")
        }

        var newState = state
        val events = mutableListOf<GameEvent>()
        val opponentId = continuation.currentOpponentId
        val handZone = ZoneKey(opponentId, Zone.HAND)
        val battlefieldZone = ZoneKey(opponentId, Zone.BATTLEFIELD)

        // Separate auras from non-auras: auras need target selection before entering
        val auraCards = mutableListOf<EntityId>()
        val nonAuraCards = mutableListOf<EntityId>()

        for (cardId in response.selectedCards) {
            if (cardId !in newState.getZone(handZone)) continue
            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            if (cardComponent?.isAura == true) {
                auraCards.add(cardId)
            } else {
                nonAuraCards.add(cardId)
            }
        }

        // Move non-aura cards from hand to battlefield immediately
        for (cardId in nonAuraCards) {
            if (cardId !in newState.getZone(handZone)) continue

            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(battlefieldZone, cardId)

            // Apply battlefield components
            val container = newState.getEntity(cardId)
            if (container != null) {
                var newContainer = container
                    .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(opponentId))

                // Creatures enter with summoning sickness
                val cardComponent = container.get<CardComponent>()
                if (cardComponent?.typeLine?.isCreature == true) {
                    newContainer = newContainer.with(
                        com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                    )
                }

                newState = newState.copy(
                    entities = newState.entities + (cardId to newContainer)
                )
            }

            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            val cardName = cardComponent?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.HAND,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = opponentId
                )
            )
        }

        // If there are auras to place, ask for target selection for the first one
        if (auraCards.isNotEmpty()) {
            return askAuraTargetForEntryFromHand(
                state = newState,
                events = events,
                auraId = auraCards.first(),
                opponentId = opponentId,
                remainingAuras = auraCards.drop(1),
                remainingOpponents = continuation.remainingOpponents,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                controllerId = continuation.controllerId,
                filter = continuation.filter
            )
        }

        // Ask the next opponent
        return proceedToNextOpponentOrFinish(
            state = newState,
            events = events,
            remainingOpponents = continuation.remainingOpponents,
            filter = continuation.filter,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId
        )
    }

    /**
     * Ask the opponent to choose a target for an Aura entering the battlefield from hand.
     * Per Rule 303.4f, the controller chooses what it enchants.
     * If no legal target exists, the Aura stays in hand (Rule 303.4g).
     */
    private fun askAuraTargetForEntryFromHand(
        state: GameState,
        events: List<GameEvent>,
        auraId: EntityId,
        opponentId: EntityId,
        remainingAuras: List<EntityId>,
        remainingOpponents: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): ExecutionResult {
        val cardComponent = state.getEntity(auraId)?.get<CardComponent>()
        val cardDef = cardComponent?.let { stackResolver.cardRegistry?.getCard(it.cardDefinitionId) }
        val auraTarget = cardDef?.script?.auraTarget

        if (auraTarget == null) {
            // No aura target defined — skip this aura (leave in hand)
            return continueAuraProcessingOrProceed(
                state = state,
                events = events,
                remainingAuras = remainingAuras,
                opponentId = opponentId,
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter
            )
        }

        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = auraTarget,
            controllerId = opponentId,
            sourceId = auraId
        )

        if (legalTargets.isEmpty()) {
            // No legal targets — Aura stays in hand per Rule 303.4g
            return continueAuraProcessingOrProceed(
                state = state,
                events = events,
                remainingAuras = remainingAuras,
                opponentId = opponentId,
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter
            )
        }

        // Create target selection decision for the aura
        val decisionId = java.util.UUID.randomUUID().toString()
        val auraName = cardComponent.name
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = auraTarget.description,
            minTargets = 1,
            maxTargets = 1
        )
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = opponentId,
            prompt = "Choose what $auraName enchants",
            context = DecisionContext(
                sourceId = auraId,
                sourceName = auraName,
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalTargets)
        )

        val auraContinuation = ChooseAuraTargetForEntryFromHandContinuation(
            decisionId = decisionId,
            auraId = auraId,
            opponentId = opponentId,
            remainingAuras = remainingAuras,
            remainingOpponents = remainingOpponents,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = controllerId,
            filter = filter
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(auraContinuation)

        return ExecutionResult(
            state = stateWithContinuation,
            events = events,
            pendingDecision = decision
        )
    }

    /**
     * Continue processing remaining auras, or proceed to next opponent if no more auras.
     */
    private fun continueAuraProcessingOrProceed(
        state: GameState,
        events: List<GameEvent>,
        remainingAuras: List<EntityId>,
        opponentId: EntityId,
        remainingOpponents: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): ExecutionResult {
        if (remainingAuras.isNotEmpty()) {
            return askAuraTargetForEntryFromHand(
                state = state,
                events = events,
                auraId = remainingAuras.first(),
                opponentId = opponentId,
                remainingAuras = remainingAuras.drop(1),
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter
            )
        }

        return proceedToNextOpponentOrFinish(
            state = state,
            events = events,
            remainingOpponents = remainingOpponents,
            filter = filter,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = controllerId
        )
    }

    /**
     * Ask the next opponent to put cards from hand, or finish if no more opponents.
     */
    private fun proceedToNextOpponentOrFinish(
        state: GameState,
        events: List<GameEvent>,
        remainingOpponents: List<EntityId>,
        filter: GameObjectFilter,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId
    ): ExecutionResult {
        if (remainingOpponents.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachOpponentMayPutFromHandExecutor.askNextOpponent(
                state = state,
                filter = filter,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                opponents = remainingOpponents,
                currentIndex = 0
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = events + nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        return checkForMoreContinuations(state, events)
    }

    /**
     * Resume after an opponent chose a target for an Aura entering the battlefield from hand.
     * Moves the aura from hand to battlefield with AttachedToComponent, then continues
     * processing remaining auras or the next opponent.
     */
    private fun resumeChooseAuraTargetForEntryFromHand(
        state: GameState,
        continuation: ChooseAuraTargetForEntryFromHandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for aura target selection")
        }

        val targetIds = response.selectedTargets[0] ?: emptyList()
        if (targetIds.isEmpty()) {
            return ExecutionResult.error(state, "No target selected for aura")
        }

        val targetId = targetIds.first()
        val auraId = continuation.auraId
        val opponentId = continuation.opponentId
        val handZone = ZoneKey(opponentId, Zone.HAND)
        val battlefieldZone = ZoneKey(opponentId, Zone.BATTLEFIELD)

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Verify aura is still in hand
        if (auraId !in newState.getZone(handZone)) {
            // Aura is no longer in hand — skip
            return continueAuraProcessingOrProceed(
                state = newState,
                events = events,
                remainingAuras = continuation.remainingAuras,
                opponentId = opponentId,
                remainingOpponents = continuation.remainingOpponents,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                controllerId = continuation.controllerId,
                filter = continuation.filter
            )
        }

        // Move aura from hand to battlefield
        newState = newState.removeFromZone(handZone, auraId)
        newState = newState.addToZone(battlefieldZone, auraId)

        // Apply battlefield components + AttachedToComponent
        val container = newState.getEntity(auraId)
        if (container != null) {
            val newContainer = container
                .with(ControllerComponent(opponentId))
                .with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(targetId))

            newState = newState.copy(
                entities = newState.entities + (auraId to newContainer)
            )
        }

        val cardComponent = newState.getEntity(auraId)?.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"
        events.add(
            ZoneChangeEvent(
                entityId = auraId,
                entityName = cardName,
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = opponentId
            )
        )

        // Continue with remaining auras or next opponent
        return continueAuraProcessingOrProceed(
            state = newState,
            events = events,
            remainingAuras = continuation.remainingAuras,
            opponentId = opponentId,
            remainingOpponents = continuation.remainingOpponents,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            filter = continuation.filter
        )
    }

    /**
     * Resume after a player selected creature cards from their hand to reveal.
     *
     * Records the reveal count, then asks the next player (if any).
     * After all players have selected, creates tokens for each player.
     */
    private fun resumeEachPlayerMayRevealCreatures(
        state: GameState,
        continuation: EachPlayerMayRevealCreaturesContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-player-may-reveal-creatures")
        }

        val playerId = continuation.currentPlayerId
        val revealCount = response.selectedCards.size

        // Update reveal counts
        val updatedRevealCounts = if (revealCount > 0) {
            continuation.revealCounts + (playerId to revealCount)
        } else {
            continuation.revealCounts
        }

        // Ask the next player
        val remainingPlayers = continuation.remainingPlayers
        if (remainingPlayers.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.askNextPlayer(
                state = state,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                players = remainingPlayers,
                currentIndex = 0,
                revealCounts = updatedRevealCounts,
                tokenPower = continuation.tokenPower,
                tokenToughness = continuation.tokenToughness,
                tokenColors = continuation.tokenColors,
                tokenCreatureTypes = continuation.tokenCreatureTypes,
                tokenImageUri = continuation.tokenImageUri
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        // All players have made their selection - create tokens
        val tokenResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.createTokensForAllPlayers(
            state = state,
            revealCounts = updatedRevealCounts,
            tokenPower = continuation.tokenPower,
            tokenToughness = continuation.tokenToughness,
            tokenColors = continuation.tokenColors,
            tokenCreatureTypes = continuation.tokenCreatureTypes,
            tokenImageUri = continuation.tokenImageUri
        )

        return checkForMoreContinuations(tokenResult.newState, tokenResult.events)
    }

    /**
     * Resume after a player selects cards from library in "each player searches library" effect.
     *
     * Moves selected cards to hand, reveals them, shuffles library, then asks the next player.
     */
    private fun resumeEachPlayerSearchesLibrary(
        state: GameState,
        continuation: EachPlayerSearchesLibraryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-player-searches-library")
        }

        val playerId = continuation.currentPlayerId
        val selectedCards = response.selectedCards
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to hand
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
        }

        // Reveal cards if any were selected
        if (selectedCards.isNotEmpty()) {
            val cardNames = selectedCards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = selectedCards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = selectedCards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = continuation.sourceName
                )
            )
            for (i in selectedCards.indices) {
                events.add(
                    ZoneChangeEvent(
                        entityId = selectedCards[i],
                        entityName = cardNames[i],
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.HAND,
                        ownerId = playerId
                    )
                )
            }
        }

        // Shuffle the library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))
        events.add(LibraryShuffledEvent(playerId))

        // Ask next player
        val remainingPlayers = continuation.remainingPlayers
        if (remainingPlayers.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerSearchesLibraryExecutor.askNextPlayer(
                state = newState,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                players = remainingPlayers,
                currentIndex = 0,
                filter = continuation.filter,
                maxCount = continuation.maxCount
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = events + nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        // All players done
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for "must attack this turn" effect.
     *
     * Marks all creatures of the chosen type on the battlefield with MustAttackThisTurnComponent.
     */
    private fun resumeChooseCreatureTypeMustAttack(
        state: GameState,
        continuation: ChooseCreatureTypeMustAttackContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in newState.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) continue

            // Must have the chosen subtype
            if (!cardComponent.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))) continue

            // Add MustAttackThisTurnComponent
            newState = newState.updateEntity(entityId) { it.with(
                com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
            ) }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player chose a creature type for "untap all creatures of that type" effect.
     *
     * Untaps all creatures of the chosen type on the battlefield.
     */
    private fun resumeChooseCreatureTypeUntap(
        state: GameState,
        continuation: ChooseCreatureTypeUntapContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in newState.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) continue

            // Must have the chosen subtype
            if (!cardComponent.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))) continue

            // Skip already untapped creatures
            if (!container.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()) continue

            // Untap the creature
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()
            }
            events.add(UntappedEvent(entityId, cardComponent.name))
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after a player chose a creature type for Harsh Mercy.
     *
     * Records the chosen type, asks the next player if any remain,
     * or destroys all creatures not of any chosen type.
     */
    private fun resumeHarshMercy(
        state: GameState,
        continuation: HarshMercyContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val updatedChosenTypes = continuation.chosenTypes + chosenType

        // If there are more players, ask the next one
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemaining = continuation.remainingPlayers.drop(1)

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = nextPlayer,
                prompt = "Choose a creature type",
                context = DecisionContext(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = continuation.creatureTypes
            )

            val newContinuation = continuation.copy(
                decisionId = decisionId,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemaining,
                chosenTypes = updatedChosenTypes
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = nextPlayer,
                        decisionType = "CHOOSE_OPTION",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // All players have chosen — destroy creatures not of any chosen type
        val chosenSubtypes = updatedChosenTypes.map { com.wingedsheep.sdk.core.Subtype(it) }.toSet()

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Only affects creatures
            if (!cardComponent.typeLine.isCreature) continue

            // Check if creature has any of the chosen subtypes
            val hasChosenType = cardComponent.typeLine.subtypes.any { it in chosenSubtypes }
            if (hasChosenType) continue

            // Destroy (can't be regenerated)
            val result = com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent(
                newState, entityId, canRegenerate = false
            )
            newState = result.newState
            events.addAll(result.events)
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after a player chooses a creature type for Patriarch's Bidding.
     * If more players remain, ask the next one. Otherwise, return all creature cards
     * matching any chosen type from all graveyards to the battlefield.
     */
    private fun resumePatriarchsBidding(
        state: GameState,
        continuation: PatriarchsBiddingContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val updatedChosenTypes = continuation.chosenTypes + chosenType

        // If there are more players, ask the next one
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemaining = continuation.remainingPlayers.drop(1)

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = nextPlayer,
                prompt = "Choose a creature type",
                context = DecisionContext(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = continuation.creatureTypes
            )

            val newContinuation = continuation.copy(
                decisionId = decisionId,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemaining,
                chosenTypes = updatedChosenTypes
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = nextPlayer,
                        decisionType = "CHOOSE_OPTION",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // All players have chosen — return matching creatures from all graveyards to battlefield
        val chosenSubtypes = updatedChosenTypes.map { com.wingedsheep.sdk.core.Subtype(it) }.toSet()

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Process each player's graveyard
        for (playerId in state.turnOrder) {
            val graveyardZone = com.wingedsheep.engine.state.ZoneKey(playerId, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
            val graveyard = newState.getZone(graveyardZone).toList() // snapshot to avoid concurrent modification

            for (entityId in graveyard) {
                val container = newState.getEntity(entityId) ?: continue
                val cardComponent = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>() ?: continue

                // Only creature cards
                if (!cardComponent.typeLine.isCreature) continue

                // Check if creature has any of the chosen subtypes
                val hasChosenType = cardComponent.typeLine.subtypes.any { it in chosenSubtypes }
                if (!hasChosenType) continue

                // Move from graveyard to battlefield
                val result = com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.moveCardToZone(
                    newState, entityId, com.wingedsheep.sdk.core.Zone.BATTLEFIELD
                )
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after the destroyed permanent's controller decides whether to copy Chain of Acid.
     */
    private fun resumeChainCopyDecision(
        state: GameState,
        continuation: ChainCopyDecisionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for chain copy decision")
        }

        if (!response.choice) {
            // Player declined — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        // Player wants to copy — find legal noncreature permanent targets
        val requirement = com.wingedsheep.sdk.targeting.TargetPermanent(filter = continuation.targetFilter)
        val legalTargets = targetFinder.findLegalTargets(
            state, requirement, continuation.targetControllerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            // No valid targets — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        // Present target selection
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a target for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = ChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the copying player selects a target for the chain copy.
     * Creates a triggered ability on the stack with DestroyAndChainCopyEffect.
     */
    private fun resumeChainCopyTarget(
        state: GameState,
        continuation: ChainCopyTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        // Validate the selected target is among the legal targets
        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid chain copy target: $selectedTarget")
        }

        // Create a copy of the spell on the stack as a triggered ability
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = com.wingedsheep.sdk.scripting.DestroyAndChainCopyEffect(
                target = com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.targeting.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMoreContinuations(putResult.state, putResult.events.toList())
    }

    /**
     * Resume after the bounced permanent's controller decides whether to sacrifice a land
     * to copy Chain of Vapor.
     */
    private fun resumeBounceChainCopyDecision(
        state: GameState,
        continuation: BounceChainCopyDecisionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for bounce chain copy decision")
        }

        if (!response.choice) {
            // Player declined — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        // Player wants to copy — find their lands to sacrifice
        val controllerLands = findControllerLands(state, continuation.targetControllerId)

        if (controllerLands.isEmpty()) {
            // No lands to sacrifice — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        if (controllerLands.size == 1) {
            // Only one land — auto-sacrifice it and proceed to target selection
            return sacrificeLandAndPresentTargets(
                state, continuation.targetControllerId, controllerLands.first(),
                continuation.targetFilter, continuation.spellName, continuation.sourceId
            )
        }

        // Multiple lands — player must choose which to sacrifice
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a land to sacrifice for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = controllerLands,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val landContinuation = BounceChainCopyLandContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateLands = controllerLands
        )

        val newState = state.withPendingDecision(decision).pushContinuation(landContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the copying player selects which land to sacrifice for the bounce chain copy.
     */
    private fun resumeBounceChainCopyLand(
        state: GameState,
        continuation: BounceChainCopyLandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for bounce chain land sacrifice")
        }

        val selectedLand = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedLand !in continuation.candidateLands) {
            return ExecutionResult.error(state, "Invalid land for bounce chain sacrifice: $selectedLand")
        }

        return sacrificeLandAndPresentTargets(
            state, continuation.copyControllerId, selectedLand,
            continuation.targetFilter, continuation.spellName, continuation.sourceId
        )
    }

    /**
     * Resume after the copying player selects a target for the bounce chain copy.
     * Creates a triggered ability on the stack with BounceAndChainCopyEffect.
     */
    private fun resumeBounceChainCopyTarget(
        state: GameState,
        continuation: BounceChainCopyTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for bounce chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid bounce chain copy target: $selectedTarget")
        }

        // Create a copy of the spell on the stack as a triggered ability
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = com.wingedsheep.sdk.scripting.BounceAndChainCopyEffect(
                target = com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.targeting.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMoreContinuations(putResult.state, putResult.events.toList())
    }

    /**
     * Sacrifice a land and present target selection for the bounce chain copy.
     * Shared logic used by both auto-sacrifice (single land) and manual selection paths.
     */
    private fun sacrificeLandAndPresentTargets(
        state: GameState,
        controllerId: EntityId,
        landId: EntityId,
        targetFilter: com.wingedsheep.sdk.scripting.TargetFilter,
        spellName: String,
        sourceId: EntityId?
    ): ExecutionResult {
        // Sacrifice the land
        val container = state.getEntity(landId) ?: return checkForMoreContinuations(state, emptyList())
        val cardComponent = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?: return checkForMoreContinuations(state, emptyList())

        val currentZone = state.zones.entries.find { (_, cards) -> landId in cards }?.key
            ?: return checkForMoreContinuations(state, emptyList())

        val ownerId = container.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: controllerId
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(ownerId, com.wingedsheep.sdk.core.Zone.GRAVEYARD)

        var newState = state.removeFromZone(currentZone, landId)
        newState = newState.addToZone(graveyardZone, landId)

        val sacrificeEvents = mutableListOf<GameEvent>(
            PermanentsSacrificedEvent(controllerId, listOf(landId)),
            ZoneChangeEvent(
                entityId = landId,
                entityName = cardComponent.name,
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = ownerId
            )
        )

        // Now find legal nonland permanent targets for the copy
        val requirement = com.wingedsheep.sdk.targeting.TargetPermanent(filter = targetFilter)
        val legalTargets = targetFinder.findLegalTargets(newState, requirement, controllerId, sourceId)

        if (legalTargets.isEmpty()) {
            // No valid targets after sacrifice — chain ends
            return checkForMoreContinuations(newState, sacrificeEvents)
        }

        // Present target selection
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of $spellName",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = BounceChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = controllerId,
            targetFilter = targetFilter,
            spellName = spellName,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            sacrificeEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after player selected cards to discard for Chain of Smog.
     * Discards the selected cards, then offers the chain copy decision.
     */
    private fun resumeDiscardForChain(
        state: GameState,
        continuation: DiscardForChainContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard chain")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Now offer the chain copy
        val requirement = com.wingedsheep.sdk.targeting.TargetPlayer()
        val legalTargets = targetFinder.findLegalTargets(
            newState, requirement, playerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMoreContinuations(newState, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Copy ${continuation.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Copy",
            noText = "Decline"
        )

        val copyContinuation = DiscardChainCopyDecisionContinuation(
            decisionId = decisionId,
            targetPlayerId = playerId,
            count = continuation.count,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId
        )

        newState = newState.withPendingDecision(decision).pushContinuation(copyContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the discarding player decides whether to copy Chain of Smog.
     */
    private fun resumeDiscardChainCopyDecision(
        state: GameState,
        continuation: DiscardChainCopyDecisionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for discard chain copy decision")
        }

        if (!response.choice) {
            // Player declined — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        // Player wants to copy — find legal player targets
        val requirement = com.wingedsheep.sdk.targeting.TargetPlayer()
        val legalTargets = targetFinder.findLegalTargets(
            state, requirement, continuation.targetPlayerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        // Present target selection
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetPlayerId,
            prompt = "Choose a target player for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = DiscardChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetPlayerId,
            count = continuation.count,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetPlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the copying player selects a target player for the discard chain copy.
     * Creates a triggered ability on the stack with DiscardAndChainCopyEffect.
     */
    private fun resumeDiscardChainCopyTarget(
        state: GameState,
        continuation: DiscardChainCopyTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid discard chain copy target: $selectedTarget")
        }

        // Create a copy of the spell on the stack as a triggered ability
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = com.wingedsheep.sdk.scripting.DiscardAndChainCopyEffect(
                count = continuation.count,
                target = com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget(0),
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Player(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.targeting.TargetPlayer()
        )

        val putResult = stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMoreContinuations(putResult.state, putResult.events.toList())
    }

    /**
     * Resume after the damaged player/permanent's controller decides whether to
     * discard a card to copy Chain of Plasma.
     */
    private fun resumeDamageChainCopyDecision(
        state: GameState,
        continuation: DamageChainCopyDecisionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for damage chain copy decision")
        }

        if (!response.choice) {
            // Player declined — chain ends
            return checkForMoreContinuations(state, emptyList())
        }

        // Player wants to discard and copy — present card selection for discard
        val handZone = ZoneKey(continuation.affectedPlayerId, Zone.HAND)
        val hand = state.getZone(handZone)

        if (hand.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        val sourceName = continuation.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: continuation.spellName

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.affectedPlayerId,
            prompt = "Choose a card to discard for ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = hand,
            minSelections = 1,
            maxSelections = 1
        )

        val discardContinuation = DamageChainDiscardContinuation(
            decisionId = decisionId,
            affectedPlayerId = continuation.affectedPlayerId,
            amount = continuation.amount,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(discardContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.affectedPlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the affected player selects a card to discard for the damage chain copy.
     * Discards the card, then presents target selection for the copy.
     */
    private fun resumeDamageChainDiscard(
        state: GameState,
        continuation: DamageChainDiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for damage chain discard")
        }

        val selectedCard = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        // Discard the selected card
        val playerId = continuation.affectedPlayerId
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(handZone, selectedCard)
        newState = newState.addToZone(graveyardZone, selectedCard)

        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(playerId, listOf(selectedCard))
        )

        // Now present target selection for the copy
        val requirement = com.wingedsheep.sdk.targeting.AnyTarget()
        val legalTargets = targetFinder.findLegalTargets(
            newState, requirement, playerId, continuation.sourceId
        )

        if (legalTargets.isEmpty()) {
            return checkForMoreContinuations(newState, events)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose a target for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = DamageChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = playerId,
            amount = continuation.amount,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the copying player selects a target for the damage chain copy.
     * Creates a triggered ability on the stack with DamageAndChainCopyEffect.
     */
    private fun resumeDamageChainCopyTarget(
        state: GameState,
        continuation: DamageChainCopyTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for damage chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid damage chain copy target: $selectedTarget")
        }

        // Determine the ChosenTarget type based on what was selected
        val entity = state.getEntity(selectedTarget)
        val chosenTarget = if (entity?.get<ControllerComponent>() != null) {
            ChosenTarget.Permanent(selectedTarget)
        } else {
            ChosenTarget.Player(selectedTarget)
        }

        // Create a copy of the spell on the stack as a triggered ability
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = com.wingedsheep.sdk.scripting.DamageAndChainCopyEffect(
                amount = continuation.amount,
                target = com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget(0),
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(chosenTarget)
        val targetRequirements = listOf(
            com.wingedsheep.sdk.targeting.AnyTarget()
        )

        val putResult = stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMoreContinuations(putResult.state, putResult.events.toList())
    }

    /**
     * Resume after the target creature's controller decides whether to sacrifice a land
     * to copy Chain of Silence.
     */
    private fun resumePreventDamageChainCopyDecision(
        state: GameState,
        continuation: PreventDamageChainCopyDecisionContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for prevent damage chain copy decision")
        }

        if (!response.choice) {
            return checkForMoreContinuations(state, emptyList())
        }

        val controllerLands = findControllerLands(state, continuation.targetControllerId)

        if (controllerLands.isEmpty()) {
            return checkForMoreContinuations(state, emptyList())
        }

        if (controllerLands.size == 1) {
            return sacrificeLandAndPresentPreventDamageChainTargets(
                state, continuation.targetControllerId, controllerLands.first(),
                continuation.targetFilter, continuation.spellName, continuation.sourceId
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = continuation.targetControllerId,
            prompt = "Choose a land to sacrifice for the copy of ${continuation.spellName}",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = controllerLands,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val landContinuation = PreventDamageChainCopyLandContinuation(
            decisionId = decisionId,
            copyControllerId = continuation.targetControllerId,
            targetFilter = continuation.targetFilter,
            spellName = continuation.spellName,
            sourceId = continuation.sourceId,
            candidateLands = controllerLands
        )

        val newState = state.withPendingDecision(decision).pushContinuation(landContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.targetControllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the copying player selects which land to sacrifice for the prevent damage chain copy.
     */
    private fun resumePreventDamageChainCopyLand(
        state: GameState,
        continuation: PreventDamageChainCopyLandContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for prevent damage chain land sacrifice")
        }

        val selectedLand = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedLand !in continuation.candidateLands) {
            return ExecutionResult.error(state, "Invalid land for prevent damage chain sacrifice: $selectedLand")
        }

        return sacrificeLandAndPresentPreventDamageChainTargets(
            state, continuation.copyControllerId, selectedLand,
            continuation.targetFilter, continuation.spellName, continuation.sourceId
        )
    }

    /**
     * Resume after the copying player selects a target for the prevent damage chain copy.
     * Creates a triggered ability on the stack with PreventDamageAndChainCopyEffect.
     */
    private fun resumePreventDamageChainCopyTarget(
        state: GameState,
        continuation: PreventDamageChainCopyTargetContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for prevent damage chain copy target")
        }

        val selectedTarget = response.selectedCards.firstOrNull()
            ?: return checkForMoreContinuations(state, emptyList())

        if (selectedTarget !in continuation.candidateTargets) {
            return ExecutionResult.error(state, "Invalid prevent damage chain copy target: $selectedTarget")
        }

        val ability = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId ?: EntityId.generate(),
            sourceName = continuation.spellName,
            controllerId = continuation.copyControllerId,
            effect = com.wingedsheep.sdk.scripting.PreventDamageAndChainCopyEffect(
                target = com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget(0),
                targetFilter = continuation.targetFilter,
                spellName = continuation.spellName
            ),
            description = "Copy of ${continuation.spellName}"
        )

        val targets = listOf(ChosenTarget.Permanent(selectedTarget))
        val targetRequirements = listOf(
            com.wingedsheep.sdk.targeting.TargetPermanent(filter = continuation.targetFilter)
        )

        val putResult = stackResolver.putTriggeredAbility(state, ability, targets, targetRequirements)

        if (!putResult.isSuccess) {
            return putResult
        }

        return checkForMoreContinuations(putResult.state, putResult.events.toList())
    }

    /**
     * Sacrifice a land and present target selection for the prevent damage chain copy.
     */
    private fun sacrificeLandAndPresentPreventDamageChainTargets(
        state: GameState,
        controllerId: EntityId,
        landId: EntityId,
        targetFilter: com.wingedsheep.sdk.scripting.TargetFilter,
        spellName: String,
        sourceId: EntityId?
    ): ExecutionResult {
        val container = state.getEntity(landId) ?: return checkForMoreContinuations(state, emptyList())
        val cardComponent = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?: return checkForMoreContinuations(state, emptyList())

        val currentZone = state.zones.entries.find { (_, cards) -> landId in cards }?.key
            ?: return checkForMoreContinuations(state, emptyList())

        val ownerId = container.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: controllerId
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(ownerId, com.wingedsheep.sdk.core.Zone.GRAVEYARD)

        var newState = state.removeFromZone(currentZone, landId)
        newState = newState.addToZone(graveyardZone, landId)

        val sacrificeEvents = mutableListOf<GameEvent>(
            PermanentsSacrificedEvent(controllerId, listOf(landId)),
            ZoneChangeEvent(
                entityId = landId,
                entityName = cardComponent.name,
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = ownerId
            )
        )

        val requirement = com.wingedsheep.sdk.targeting.TargetPermanent(filter = targetFilter)
        val legalTargets = targetFinder.findLegalTargets(newState, requirement, controllerId, sourceId)

        if (legalTargets.isEmpty()) {
            return checkForMoreContinuations(newState, sacrificeEvents)
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a target for the copy of $spellName",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = spellName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = legalTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val targetContinuation = PreventDamageChainCopyTargetContinuation(
            decisionId = decisionId,
            copyControllerId = controllerId,
            targetFilter = targetFilter,
            spellName = spellName,
            sourceId = sourceId,
            candidateTargets = legalTargets
        )

        newState = newState.withPendingDecision(decision).pushContinuation(targetContinuation)

        return ExecutionResult.paused(
            newState,
            decision,
            sacrificeEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after a player selects a permanent to bounce for Words of Wind's draw replacement.
     */
    private fun resumeDrawReplacementBounce(
        state: GameState,
        continuation: DrawReplacementBounceContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for bounce")
        }

        val selectedPermanent = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No permanent selected for bounce")

        // Bounce the selected permanent to its owner's hand
        val bounceResult = EffectExecutorUtils.moveCardToZone(state, selectedPermanent, Zone.HAND)
        if (!bounceResult.isSuccess) {
            return bounceResult
        }

        var newState = bounceResult.state
        val events = bounceResult.events.toMutableList()

        // Continue with remaining players
        if (continuation.remainingPlayers.isNotEmpty()) {
            val projected = StateProjector().project(newState)
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemaining = continuation.remainingPlayers.drop(1)
            val controlledPermanents = projected.getBattlefieldControlledBy(nextPlayer)

            if (controlledPermanents.isEmpty()) {
                // Skip player with no permanents, continue chain
                return resumeDrawReplacementBounceChain(
                    newState, nextRemaining, continuation, events
                )
            }

            if (controlledPermanents.size == 1) {
                // Auto-bounce single permanent
                val permanentId = controlledPermanents.first()
                val autoResult = EffectExecutorUtils.moveCardToZone(newState, permanentId, Zone.HAND)
                newState = autoResult.state
                events.addAll(autoResult.events)
                return resumeDrawReplacementBounceChain(
                    newState, nextRemaining, continuation, events
                )
            }

            // Present decision to next player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName ?: "Words of Wind",
                prompt = "Choose a permanent to return to its owner's hand",
                options = controlledPermanents,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                useTargetingUI = true
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentBouncingPlayerId = nextPlayer,
                remainingPlayers = nextRemaining
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // All players done bouncing - continue with remaining draws
        return finishDrawReplacementBounce(newState, continuation, events)
    }

    /**
     * Continue the bounce chain, skipping players with 0-1 permanents.
     */
    private fun resumeDrawReplacementBounceChain(
        state: GameState,
        remainingPlayers: List<EntityId>,
        continuation: DrawReplacementBounceContinuation,
        events: MutableList<GameEvent>
    ): ExecutionResult {
        var newState = state

        // Process remaining players, auto-bouncing or skipping as needed
        var remaining = remainingPlayers
        while (remaining.isNotEmpty()) {
            val projected = StateProjector().project(newState)
            val nextPlayer = remaining.first()
            remaining = remaining.drop(1)
            val controlledPermanents = projected.getBattlefieldControlledBy(nextPlayer)

            if (controlledPermanents.isEmpty()) continue

            if (controlledPermanents.size == 1) {
                val permanentId = controlledPermanents.first()
                val autoResult = EffectExecutorUtils.moveCardToZone(newState, permanentId, Zone.HAND)
                newState = autoResult.state
                events.addAll(autoResult.events)
                continue
            }

            // Need a decision - present to player
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = nextPlayer,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName ?: "Words of Wind",
                prompt = "Choose a permanent to return to its owner's hand",
                options = controlledPermanents,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                useTargetingUI = true
            )

            val newContinuation = continuation.copy(
                decisionId = decisionResult.pendingDecision!!.id,
                currentBouncingPlayerId = nextPlayer,
                remainingPlayers = remaining
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // All players done
        return finishDrawReplacementBounce(newState, continuation, events)
    }

    /**
     * Finish the bounce replacement by processing remaining draws.
     */
    private fun finishDrawReplacementBounce(
        state: GameState,
        continuation: DrawReplacementBounceContinuation,
        events: List<GameEvent>
    ): ExecutionResult {
        if (continuation.remainingDraws > 0) {
            val drawExecutor = DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry)
            val drawResult = drawExecutor.executeDraws(
                state, continuation.drawingPlayerId, continuation.remainingDraws
            )
            if (drawResult.isPaused) {
                // Another bounce shield was hit during remaining draws
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            return ExecutionResult(
                drawResult.state,
                events + drawResult.events,
                drawResult.error
            )
        }
        return checkForMoreContinuations(state, events)
    }

    /**
     * Resume after an opponent selects a card to discard for Words of Waste's draw replacement.
     */
    private fun resumeDrawReplacementDiscard(
        state: GameState,
        continuation: DrawReplacementDiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard")
        }

        val cardToDiscard = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No card selected for discard")

        // Discard the selected card
        val handZone = ZoneKey(continuation.discardingPlayerId, Zone.HAND)
        val graveyardZone = ZoneKey(continuation.discardingPlayerId, Zone.GRAVEYARD)
        var newState = state.removeFromZone(handZone, cardToDiscard)
        newState = newState.addToZone(graveyardZone, cardToDiscard)
        val events = mutableListOf<GameEvent>(
            CardsDiscardedEvent(continuation.discardingPlayerId, listOf(cardToDiscard))
        )

        // Continue with remaining draws
        if (continuation.remainingDraws > 0) {
            val drawExecutor = DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry)
            val drawResult = drawExecutor.executeDraws(
                newState, continuation.drawingPlayerId, continuation.remainingDraws
            )
            if (drawResult.isPaused) {
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            return ExecutionResult(
                drawResult.state,
                events + drawResult.events,
                drawResult.error
            )
        }
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after the player selects mana sources (or declines) for a "prompt on draw"
     * ability (e.g., Words of Wind). If they pay, creates a replacement shield,
     * then draws cards.
     */
    private fun resumeDrawReplacementActivation(
        state: GameState,
        continuation: DrawReplacementActivationContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources response for draw replacement activation")
        }

        val playerId = continuation.drawingPlayerId
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if the player chose to activate (non-empty sources or autoPay)
        val activated = response.autoPay || response.selectedSources.isNotEmpty()

        if (activated) {
            val manaCost = com.wingedsheep.sdk.core.ManaCost.parse(continuation.manaCost)
            val costHandler = CostHandler()

            // Get current mana pool
            val poolComponent = newState.getEntity(playerId)?.get<ManaPoolComponent>()
            var currentPool = if (poolComponent != null) {
                ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )
            } else {
                ManaPool()
            }

            // Try to pay from floating pool first
            val partialResult = currentPool.payPartial(manaCost)
            val remainingCost = partialResult.remainingCost

            if (!remainingCost.isEmpty()) {
                if (response.autoPay) {
                    // Auto-tap: use ManaSolver
                    val manaSolver = ManaSolver()
                    val solution = manaSolver.solve(newState, playerId, remainingCost)
                        ?: return ExecutionResult.error(newState, "Cannot pay mana cost for draw replacement activation")

                    for (source in solution.sources) {
                        newState = newState.updateEntity(source.entityId) { c ->
                            c.with(TappedComponent)
                        }
                        events.add(TappedEvent(source.entityId, source.name))
                    }

                    for ((_, production) in solution.manaProduced) {
                        currentPool = if (production.color != null) {
                            currentPool.add(production.color)
                        } else {
                            currentPool.addColorless(production.colorless)
                        }
                    }
                } else {
                    // Manual selection: tap the chosen sources
                    for (sourceId in response.selectedSources) {
                        val sourceEntity = newState.getEntity(sourceId) ?: continue
                        val sourceName = sourceEntity.get<CardComponent>()?.name ?: "Unknown"

                        newState = newState.updateEntity(sourceId) { c ->
                            c.with(TappedComponent)
                        }
                        events.add(TappedEvent(sourceId, sourceName))

                        // Add mana from the tapped source
                        // For simplicity, add 1 colorless mana per source
                        // (ManaSolver would be more accurate but manual selection implies the player knows what they're doing)
                        currentPool = currentPool.addColorless(1)
                    }
                }
            }

            // Pay the mana cost
            val newPool = costHandler.payManaCost(currentPool, manaCost)
                ?: return ExecutionResult.error(newState, "Cannot pay mana cost for draw replacement activation")

            // Update mana pool on state
            newState = newState.updateEntity(playerId) { c ->
                c.with(ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                ))
            }

            // Execute the effect to create a replacement shield
            val opponents = newState.turnOrder.filter { it != playerId }
            val effectContext = EffectContext(
                controllerId = playerId,
                sourceId = continuation.sourceId,
                opponentId = opponents.firstOrNull(),
                targets = emptyList()
            )
            val effectResult = effectExecutorRegistry.execute(
                newState, continuation.abilityEffect, effectContext
            )
            if (effectResult.isSuccess) {
                newState = effectResult.newState
                events.addAll(effectResult.events)
            }
        }

        // Now perform the draws
        if (continuation.isDrawStep) {
            val turnManager = TurnManager()
            val drawResult = turnManager.drawCards(newState, playerId, continuation.drawCount)
            if (drawResult.isPaused) {
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            newState = drawResult.newState
            events.addAll(drawResult.events)
            // Set priority for draw step
            newState = newState.withPriority(playerId)
        } else if (activated) {
            // Player activated - use DrawCardsExecutor with cardRegistry so it can prompt
            // again for subsequent draws (e.g., Arcanis draws 3, player can activate 3 times)
            val drawExecutor = DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry)
            val drawResult = drawExecutor.executeDraws(newState, playerId, continuation.drawCount)
            if (drawResult.isPaused) {
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            newState = drawResult.newState
            events.addAll(drawResult.events)
        } else {
            // Player declined - draw 1 card normally (the one that was prompted for),
            // then continue remaining draws with prompting enabled
            val drawExecutor = DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry)
            // Draw 1 card normally (without prompting) using a no-registry executor
            val singleDrawExecutor = DrawCardsExecutor()
            val singleDrawResult = singleDrawExecutor.executeDraws(newState, playerId, 1)
            if (singleDrawResult.isPaused) {
                return ExecutionResult.paused(
                    singleDrawResult.state,
                    singleDrawResult.pendingDecision!!,
                    events + singleDrawResult.events
                )
            }
            newState = singleDrawResult.newState
            events.addAll(singleDrawResult.events)

            // Continue remaining draws with prompting
            val remainingDraws = continuation.drawCount - 1
            if (remainingDraws > 0) {
                val drawResult = drawExecutor.executeDraws(newState, playerId, remainingDraws)
                if (drawResult.isPaused) {
                    return ExecutionResult.paused(
                        drawResult.state,
                        drawResult.pendingDecision!!,
                        events + drawResult.events
                    )
                }
                newState = drawResult.newState
                events.addAll(drawResult.events)
            }
        }

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume Read the Runes continuation after player makes a sacrifice or discard choice.
     */
    private fun resumeReadTheRunes(
        state: GameState,
        continuation: ReadTheRunesContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Read the Runes")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        return when (continuation.phase) {
            ReadTheRunesPhase.SACRIFICE_CHOICE -> {
                if (selectedCards.isNotEmpty()) {
                    // Player chose to sacrifice a permanent
                    val permanentId = selectedCards.first()
                    val result = ReadTheRunesExecutor.sacrificePermanent(state, playerId, permanentId)
                    val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                        result.state, playerId, continuation.sourceId, continuation.sourceName,
                        continuation.remainingChoices - 1
                    )
                    if (loopResult.isPaused) {
                        ExecutionResult.paused(
                            loopResult.state,
                            loopResult.pendingDecision!!,
                            result.events + loopResult.events
                        )
                    } else {
                        checkForMoreContinuations(loopResult.state, result.events + loopResult.events)
                    }
                } else {
                    // Player chose not to sacrifice - must discard a card
                    val hand = state.getZone(ZoneKey(playerId, com.wingedsheep.sdk.core.Zone.HAND))
                    if (hand.isEmpty()) {
                        // No cards to discard - skip
                        val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                            state, playerId, continuation.sourceId, continuation.sourceName,
                            continuation.remainingChoices - 1
                        )
                        if (loopResult.isPaused) {
                            return loopResult
                        }
                        checkForMoreContinuations(loopResult.state, loopResult.events)
                    } else if (hand.size == 1) {
                        // Auto-discard only card
                        val result = ReadTheRunesExecutor.discardCard(state, playerId, hand.first())
                        val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                            result.state, playerId, continuation.sourceId, continuation.sourceName,
                            continuation.remainingChoices - 1
                        )
                        if (loopResult.isPaused) {
                            ExecutionResult.paused(
                                loopResult.state,
                                loopResult.pendingDecision!!,
                                result.events + loopResult.events
                            )
                        } else {
                            checkForMoreContinuations(loopResult.state, result.events + loopResult.events)
                        }
                    } else {
                        // Present discard choice
                        val decisionResult = DecisionHandler().createCardSelectionDecision(
                            state = state,
                            playerId = playerId,
                            sourceId = continuation.sourceId,
                            sourceName = continuation.sourceName,
                            prompt = "Choose a card to discard (${continuation.remainingChoices} remaining)",
                            options = hand,
                            minSelections = 1,
                            maxSelections = 1,
                            ordered = false,
                            phase = DecisionPhase.RESOLUTION
                        )

                        val newContinuation = ReadTheRunesContinuation(
                            decisionId = decisionResult.pendingDecision!!.id,
                            playerId = playerId,
                            sourceId = continuation.sourceId,
                            sourceName = continuation.sourceName,
                            remainingChoices = continuation.remainingChoices,
                            phase = ReadTheRunesPhase.DISCARD_CHOICE
                        )

                        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

                        ExecutionResult.paused(
                            stateWithContinuation,
                            decisionResult.pendingDecision,
                            decisionResult.events
                        )
                    }
                }
            }
            ReadTheRunesPhase.DISCARD_CHOICE -> {
                if (selectedCards.isEmpty()) {
                    return ExecutionResult.error(state, "Must select a card to discard for Read the Runes")
                }
                val cardId = selectedCards.first()
                val result = ReadTheRunesExecutor.discardCard(state, playerId, cardId)
                val loopResult = ReadTheRunesExecutor.startChoiceLoop(
                    result.state, playerId, continuation.sourceId, continuation.sourceName,
                    continuation.remainingChoices - 1
                )
                if (loopResult.isPaused) {
                    ExecutionResult.paused(
                        loopResult.state,
                        loopResult.pendingDecision!!,
                        result.events + loopResult.events
                    )
                } else {
                    checkForMoreContinuations(loopResult.state, result.events + loopResult.events)
                }
            }
        }
    }

    private fun findControllerLands(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = com.wingedsheep.engine.mechanics.layers.StateProjector().project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator()
        val context = com.wingedsheep.engine.handlers.PredicateContext(controllerId = controllerId)
        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matches(state, permanentId, com.wingedsheep.sdk.scripting.GameObjectFilter.Land, context)
        }
    }
}
