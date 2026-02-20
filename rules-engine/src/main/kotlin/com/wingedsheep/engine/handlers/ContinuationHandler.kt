package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.continuations.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.drawing.EachOpponentDiscardsExecutor
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 *
 * Delegates to specialized resumer modules for each continuation category.
 */
class ContinuationHandler(
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver = StackResolver(),
    private val triggerProcessor: com.wingedsheep.engine.event.TriggerProcessor? = null,
    private val triggerDetector: com.wingedsheep.engine.event.TriggerDetector? = null,
    private val combatManager: CombatManager? = null,
    private val targetFinder: TargetFinder = TargetFinder()
) {

    private val ctx = ContinuationContext(
        effectExecutorRegistry = effectExecutorRegistry,
        stackResolver = stackResolver,
        triggerProcessor = triggerProcessor,
        triggerDetector = triggerDetector,
        combatManager = combatManager,
        targetFinder = targetFinder
    )

    private val combatResumer = CombatContinuationResumer(ctx)
    private val colorChoiceResumer = ColorChoiceContinuationResumer(ctx)
    private val chainSpellResumer = ChainSpellContinuationResumer(ctx)
    private val creatureTypeResumer = CreatureTypeChoiceContinuationResumer(ctx)
    private val drawReplacementResumer = DrawReplacementContinuationResumer(ctx, ::entityIdToChosenTarget)
    private val cardSpecificResumer = CardSpecificContinuationResumer(ctx)
    private val discardAndDrawResumer = DiscardAndDrawContinuationResumer(ctx)
    private val sacrificeAndPayResumer = SacrificeAndPayContinuationResumer(ctx)
    private val manaPaymentResumer = ManaPaymentContinuationResumer(ctx)
    private val libraryAndZoneResumer = LibraryAndZoneContinuationResumer(ctx)
    private val modalAndCloneResumer = ModalAndCloneContinuationResumer(ctx)

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
            return ExecutionResult.success(state)
        }

        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        val cfm = ::checkForMoreContinuations

        return when (continuation) {
            // Core engine plumbing (kept in this class)
            is EffectContinuation -> resumeEffect(stateAfterPop, continuation, response)
            is TriggeredAbilityContinuation -> resumeTriggeredAbility(stateAfterPop, continuation, response)
            is ResolveSpellContinuation -> resumeSpellResolution(stateAfterPop, continuation, response)
            is MayAbilityContinuation -> resumeMayAbility(stateAfterPop, continuation, response)
            is MayTriggerContinuation -> resumeMayTrigger(stateAfterPop, continuation, response)
            is PendingTriggersContinuation -> {
                ExecutionResult.error(state, "PendingTriggersContinuation should not be at top of stack during decision resume")
            }

            // Combat
            is DamageAssignmentContinuation -> combatResumer.resumeDamageAssignment(stateAfterPop, continuation, response)
            is DamagePreventionContinuation -> combatResumer.resumeDamagePrevention(stateAfterPop, continuation, response, cfm)
            is BlockerOrderContinuation -> combatResumer.resumeBlockerOrder(stateAfterPop, continuation, response, cfm)
            is DistributeDamageContinuation -> combatResumer.resumeDistributeDamage(stateAfterPop, continuation, response, cfm)

            // Color choice
            is ChooseColorProtectionContinuation -> colorChoiceResumer.resumeChooseColorProtection(stateAfterPop, continuation, response, cfm)
            is ChooseColorProtectionTargetContinuation -> colorChoiceResumer.resumeChooseColorProtectionTarget(stateAfterPop, continuation, response, cfm)

            // Chain spells
            is ChainCopyDecisionContinuation -> chainSpellResumer.resumeChainCopyDecision(stateAfterPop, continuation, response, cfm)
            is ChainCopyTargetContinuation -> chainSpellResumer.resumeChainCopyTarget(stateAfterPop, continuation, response, cfm)
            is BounceChainCopyDecisionContinuation -> chainSpellResumer.resumeBounceChainCopyDecision(stateAfterPop, continuation, response, cfm)
            is BounceChainCopyLandContinuation -> chainSpellResumer.resumeBounceChainCopyLand(stateAfterPop, continuation, response, cfm)
            is BounceChainCopyTargetContinuation -> chainSpellResumer.resumeBounceChainCopyTarget(stateAfterPop, continuation, response, cfm)
            is DiscardForChainContinuation -> chainSpellResumer.resumeDiscardForChain(stateAfterPop, continuation, response, cfm)
            is DiscardChainCopyDecisionContinuation -> chainSpellResumer.resumeDiscardChainCopyDecision(stateAfterPop, continuation, response, cfm)
            is DiscardChainCopyTargetContinuation -> chainSpellResumer.resumeDiscardChainCopyTarget(stateAfterPop, continuation, response, cfm)
            is DamageChainCopyDecisionContinuation -> chainSpellResumer.resumeDamageChainCopyDecision(stateAfterPop, continuation, response, cfm)
            is DamageChainDiscardContinuation -> chainSpellResumer.resumeDamageChainDiscard(stateAfterPop, continuation, response, cfm)
            is DamageChainCopyTargetContinuation -> chainSpellResumer.resumeDamageChainCopyTarget(stateAfterPop, continuation, response, cfm)
            is PreventDamageChainCopyDecisionContinuation -> chainSpellResumer.resumePreventDamageChainCopyDecision(stateAfterPop, continuation, response, cfm)
            is PreventDamageChainCopyLandContinuation -> chainSpellResumer.resumePreventDamageChainCopyLand(stateAfterPop, continuation, response, cfm)
            is PreventDamageChainCopyTargetContinuation -> chainSpellResumer.resumePreventDamageChainCopyTarget(stateAfterPop, continuation, response, cfm)

            // Creature type choices
            is ChooseFromCreatureTypeContinuation -> creatureTypeResumer.resumeChooseFromCreatureType(stateAfterPop, continuation, response, cfm)
            is ChooseToCreatureTypeContinuation -> creatureTypeResumer.resumeChooseToCreatureType(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypePipelineContinuation -> creatureTypeResumer.resumeChooseCreatureTypePipeline(stateAfterPop, continuation, response, cfm)
            is BecomeCreatureTypeContinuation -> creatureTypeResumer.resumeBecomeCreatureType(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypeModifyStatsContinuation -> creatureTypeResumer.resumeChooseCreatureTypeModifyStats(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypeGainControlContinuation -> creatureTypeResumer.resumeChooseCreatureTypeGainControl(stateAfterPop, continuation, response, cfm)
            is BecomeChosenTypeAllCreaturesContinuation -> creatureTypeResumer.resumeBecomeChosenTypeAllCreatures(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypeMustAttackContinuation -> creatureTypeResumer.resumeChooseCreatureTypeMustAttack(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypeUntapContinuation -> creatureTypeResumer.resumeChooseCreatureTypeUntap(stateAfterPop, continuation, response, cfm)
            is HarshMercyContinuation -> creatureTypeResumer.resumeHarshMercy(stateAfterPop, continuation, response, cfm)
            is PatriarchsBiddingContinuation -> creatureTypeResumer.resumePatriarchsBidding(stateAfterPop, continuation, response, cfm)

            // Draw replacements
            is DrawReplacementRemainingDrawsContinuation -> {
                ExecutionResult.error(state, "DrawReplacementRemainingDrawsContinuation should not be at top of stack during decision resume")
            }
            is DrawReplacementActivationContinuation -> drawReplacementResumer.resumeDrawReplacementActivation(stateAfterPop, continuation, response, cfm)
            is DrawReplacementTargetContinuation -> drawReplacementResumer.resumeDrawReplacementTarget(stateAfterPop, continuation, response, cfm)

            // Card-specific
            is SecretBidContinuation -> cardSpecificResumer.resumeSecretBid(stateAfterPop, continuation, response, cfm)
            is ReadTheRunesContinuation -> cardSpecificResumer.resumeReadTheRunes(stateAfterPop, continuation, response, cfm)
            is TradeSecretsContinuation -> cardSpecificResumer.resumeTradeSecrets(stateAfterPop, continuation, response, cfm)

            // Discard and draw
            is DiscardContinuation -> discardAndDrawResumer.resumeDiscard(stateAfterPop, continuation, response, cfm)
            is HandSizeDiscardContinuation -> discardAndDrawResumer.resumeHandSizeDiscard(stateAfterPop, continuation, response, cfm)
            is EachPlayerDiscardsOrLoseLifeContinuation -> discardAndDrawResumer.resumeEachPlayerDiscardsOrLoseLife(stateAfterPop, continuation, response, cfm)
            is EachPlayerChoosesDrawContinuation -> discardAndDrawResumer.resumeEachPlayerChoosesDraw(stateAfterPop, continuation, response, cfm)

            // Sacrifice and pay
            is SacrificeContinuation -> sacrificeAndPayResumer.resumeSacrifice(stateAfterPop, continuation, response, cfm)
            is PayOrSufferContinuation -> sacrificeAndPayResumer.resumePayOrSuffer(stateAfterPop, continuation, response, cfm)
            is AnyPlayerMayPayContinuation -> sacrificeAndPayResumer.resumeAnyPlayerMayPay(stateAfterPop, continuation, response, cfm)
            is UntapChoiceContinuation -> sacrificeAndPayResumer.resumeUntapChoice(stateAfterPop, continuation, response, cfm)

            // Mana payment
            is CounterUnlessPaysContinuation -> manaPaymentResumer.resumeCounterUnlessPays(stateAfterPop, continuation, response, cfm)
            is ChangeSpellTargetContinuation -> manaPaymentResumer.resumeChangeSpellTarget(stateAfterPop, continuation, response, cfm)
            is MayPayManaContinuation -> manaPaymentResumer.resumeMayPayMana(stateAfterPop, continuation, response, cfm)
            is MayPayManaTriggerContinuation -> manaPaymentResumer.resumeMayPayManaTrigger(stateAfterPop, continuation, response, cfm)
            is ManaSourceSelectionContinuation -> manaPaymentResumer.resumeManaSourceSelection(stateAfterPop, continuation, response, cfm)

            // Library and zone
            is ReturnFromGraveyardContinuation -> libraryAndZoneResumer.resumeReturnFromGraveyard(stateAfterPop, continuation, response, cfm)
            is MoveCollectionOrderContinuation -> libraryAndZoneResumer.resumeMoveCollectionOrder(stateAfterPop, continuation, response, cfm)
            is PutOnBottomOfLibraryContinuation -> libraryAndZoneResumer.resumePutOnBottomOfLibrary(stateAfterPop, continuation, response, cfm)
            is ForEachTargetContinuation -> {
                ExecutionResult.error(state, "ForEachTargetContinuation should not be at top of stack during decision resume")
            }
            is ForEachPlayerContinuation -> {
                ExecutionResult.error(state, "ForEachPlayerContinuation should not be at top of stack during decision resume")
            }
            is PutFromHandContinuation -> libraryAndZoneResumer.resumePutFromHand(stateAfterPop, continuation, response, cfm)
            is SelectFromCollectionContinuation -> libraryAndZoneResumer.resumeSelectFromCollection(stateAfterPop, continuation, response, cfm)
            is SelectTargetPipelineContinuation -> libraryAndZoneResumer.resumeSelectTargetPipeline(stateAfterPop, continuation, response, cfm)
            is MoveCollectionAuraTargetContinuation -> libraryAndZoneResumer.resumeMoveCollectionAuraTarget(stateAfterPop, continuation, response, cfm)

            // Modal and clone
            is ModalContinuation -> modalAndCloneResumer.resumeModal(stateAfterPop, continuation, response, cfm)
            is ModalTargetContinuation -> modalAndCloneResumer.resumeModalTarget(stateAfterPop, continuation, response, cfm)
            is CloneEntersContinuation -> modalAndCloneResumer.resumeCloneEnters(stateAfterPop, continuation, response, cfm)
            is ChooseColorEntersContinuation -> modalAndCloneResumer.resumeChooseColorEnters(stateAfterPop, continuation, response, cfm)
            is ChooseCreatureTypeEntersContinuation -> modalAndCloneResumer.resumeChooseCreatureTypeEnters(stateAfterPop, continuation, response, cfm)
            is CastWithCreatureTypeContinuation -> modalAndCloneResumer.resumeCastWithCreatureType(stateAfterPop, continuation, response, cfm)
        }
    }

    // ─── Core engine methods (kept here, tightly coupled to checkForMoreContinuations) ───

    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        var currentContext = continuation.toEffectContext()
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, effect) in continuation.remainingEffects.withIndex()) {
            val stillRemaining = continuation.remainingEffects.drop(index + 1)

            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = stillRemaining,
                    sourceId = continuation.sourceId,
                    controllerId = continuation.controllerId,
                    opponentId = continuation.opponentId,
                    xValue = continuation.xValue,
                    targets = continuation.targets,
                    storedCollections = currentContext.storedCollections,
                    chosenCreatureType = continuation.chosenCreatureType
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
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
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    storedCollections = currentContext.storedCollections + result.updatedCollections
                )
            }
        }

        return checkForMoreContinuations(currentState, allEvents)
    }

    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
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

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId
        )

        val stackResult = stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMoreContinuations(stackResult.newState, stackResult.events.toList())
    }

    private fun resumeMayTrigger(
        state: GameState,
        continuation: MayTriggerContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may trigger")
        }

        if (!response.choice) {
            return checkForMoreContinuations(state, emptyList())
        }

        val trigger = continuation.trigger
        val mayEffect = trigger.ability.effect as com.wingedsheep.sdk.scripting.MayEffect
        val innerEffect = mayEffect.effect

        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        val processor = triggerProcessor
            ?: return ExecutionResult.error(state, "TriggerProcessor not available for may trigger continuation")

        val result = processor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        return checkForMoreContinuations(result.newState, result.events.toList())
    }

    private fun resumeSpellResolution(
        state: GameState,
        continuation: ResolveSpellContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        return ExecutionResult.success(state)
    }

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
            return checkForMoreContinuations(state, emptyList())
        }

        val result = effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            return result
        }

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    // ─── Central coordinator ───

    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val nextContinuation = state.peekContinuation()

        if (nextContinuation is PendingTriggersContinuation && triggerProcessor != null) {
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

        if (nextContinuation is ForEachTargetContinuation && nextContinuation.remainingTargets.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            val forEachTargetExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachTargetExecutor { s, e, c ->
                effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = EffectContext(
                sourceId = nextContinuation.sourceId,
                controllerId = nextContinuation.controllerId,
                opponentId = nextContinuation.opponentId,
                xValue = nextContinuation.xValue,
                targets = nextContinuation.remainingTargets
            )
            val result = forEachTargetExecutor.processTargets(
                stateAfterPop,
                nextContinuation.effects,
                nextContinuation.remainingTargets,
                outerContext
            )

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            // Recursively check for more continuations
            return checkForMoreContinuations(result.state, events.toMutableList().apply { addAll(result.events) })
        }

        if (nextContinuation is ForEachPlayerContinuation && nextContinuation.remainingPlayers.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            val forEachPlayerExecutor = com.wingedsheep.engine.handlers.effects.composite.ForEachPlayerExecutor { s, e, c ->
                effectExecutorRegistry.execute(s, e, c)
            }
            val outerContext = EffectContext(
                sourceId = nextContinuation.sourceId,
                controllerId = nextContinuation.controllerId,
                opponentId = nextContinuation.opponentId,
                xValue = nextContinuation.xValue
            )
            val result = forEachPlayerExecutor.processPlayers(
                stateAfterPop,
                nextContinuation.effects,
                nextContinuation.remainingPlayers,
                outerContext
            )

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            // Recursively check for more continuations
            return checkForMoreContinuations(result.state, events.toMutableList().apply { addAll(result.events) })
        }

        if (nextContinuation is DrawReplacementRemainingDrawsContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            if (nextContinuation.remainingDraws > 0) {
                if (nextContinuation.isDrawStep) {
                    val turnManager = com.wingedsheep.engine.core.TurnManager(cardRegistry = stackResolver.cardRegistry, effectExecutor = effectExecutorRegistry::execute)
                    val drawResult = turnManager.drawCards(stateAfterPop, nextContinuation.drawingPlayerId, nextContinuation.remainingDraws)
                    if (drawResult.isPaused) {
                        return ExecutionResult.paused(
                            drawResult.state,
                            drawResult.pendingDecision!!,
                            events + drawResult.events
                        )
                    }
                    return checkForMoreContinuations(drawResult.newState, events + drawResult.events)
                } else {
                    val drawExecutor = com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor(cardRegistry = stackResolver.cardRegistry, effectExecutor = effectExecutorRegistry::execute)
                    val drawResult = drawExecutor.executeDraws(stateAfterPop, nextContinuation.drawingPlayerId, nextContinuation.remainingDraws)
                    if (drawResult.isPaused) {
                        return ExecutionResult.paused(
                            drawResult.state,
                            drawResult.pendingDecision!!,
                            events + drawResult.events
                        )
                    }
                    return checkForMoreContinuations(drawResult.state, events + drawResult.events)
                }
            }
            return checkForMoreContinuations(stateAfterPop, events)
        }

        if (nextContinuation is EffectContinuation && nextContinuation.remainingEffects.isNotEmpty()) {
            val (_, stateAfterPop) = state.popContinuation()
            var currentContext = nextContinuation.toEffectContext()
            var currentState = stateAfterPop
            val allEvents = events.toMutableList()

            for ((index, effect) in nextContinuation.remainingEffects.withIndex()) {
                val stillRemaining = nextContinuation.remainingEffects.drop(index + 1)

                val stateForExecution = if (stillRemaining.isNotEmpty()) {
                    val remainingContinuation = EffectContinuation(
                        decisionId = "pending",
                        remainingEffects = stillRemaining,
                        sourceId = nextContinuation.sourceId,
                        controllerId = nextContinuation.controllerId,
                        opponentId = nextContinuation.opponentId,
                        xValue = nextContinuation.xValue,
                        targets = nextContinuation.targets,
                        storedCollections = currentContext.storedCollections,
                        chosenCreatureType = nextContinuation.chosenCreatureType
                    )
                    currentState.pushContinuation(remainingContinuation)
                } else {
                    currentState
                }

                val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

                if (!result.isSuccess && !result.isPaused) {
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
                    return ExecutionResult.paused(
                        result.state,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)

                if (result.updatedCollections.isNotEmpty()) {
                    currentContext = currentContext.copy(
                        storedCollections = currentContext.storedCollections + result.updatedCollections
                    )
                }
            }

            // Recursively check for more continuations (e.g., outer CompositeEffect)
            return checkForMoreContinuations(currentState, allEvents)
        }

        return ExecutionResult.success(state, events)
    }

    // ─── Utility ───

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
}
