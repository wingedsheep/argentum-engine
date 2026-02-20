package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId

class DrawReplacementContinuationResumer(
    private val ctx: ContinuationContext,
    private val entityIdToChosenTarget: (GameState, EntityId) -> ChosenTarget
) {

    /**
     * Resume after the player selects mana sources (or declines) for a "prompt on draw"
     * ability (e.g., Words of Wind). If they pay, creates a replacement shield,
     * then draws cards.
     */
    fun resumeDrawReplacementActivation(
        state: GameState,
        continuation: DrawReplacementActivationContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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

            // If the ability has target requirements, pause for target selection
            if (continuation.targetRequirements.isNotEmpty()) {
                val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
                val requirementInfos = continuation.targetRequirements.mapIndexed { index, req ->
                    val legalTargets = ctx.targetFinder.findLegalTargets(
                        state = newState,
                        requirement = req,
                        controllerId = playerId,
                        sourceId = continuation.sourceId
                    )
                    legalTargetsMap[index] = legalTargets
                    TargetRequirementInfo(
                        index = index,
                        description = req.description,
                        minTargets = req.effectiveMinCount,
                        maxTargets = req.count
                    )
                }

                val targetDecisionId = java.util.UUID.randomUUID().toString()
                val targetDecision = ChooseTargetsDecision(
                    id = targetDecisionId,
                    playerId = playerId,
                    prompt = "Choose targets for ${continuation.sourceName}",
                    context = DecisionContext(
                        sourceId = continuation.sourceId,
                        sourceName = continuation.sourceName,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    targetRequirements = requirementInfos,
                    legalTargets = legalTargetsMap
                )

                val targetContinuation = DrawReplacementTargetContinuation(
                    decisionId = targetDecisionId,
                    drawingPlayerId = playerId,
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    abilityEffect = continuation.abilityEffect,
                    drawCount = continuation.drawCount,
                    isDrawStep = continuation.isDrawStep,
                    drawnCardsSoFar = continuation.drawnCardsSoFar
                )

                val stateWithDecision = newState.withPendingDecision(targetDecision)
                val stateWithContinuation = stateWithDecision.pushContinuation(targetContinuation)

                return ExecutionResult.paused(
                    stateWithContinuation,
                    targetDecision,
                    events + listOf(
                        DecisionRequestedEvent(
                            decisionId = targetDecisionId,
                            playerId = playerId,
                            decisionType = "CHOOSE_TARGETS",
                            prompt = targetDecision.prompt
                        )
                    )
                )
            }

            // Execute the effect to create a replacement shield (no targeting needed)
            val opponents = newState.turnOrder.filter { it != playerId }
            val effectContext = EffectContext(
                controllerId = playerId,
                sourceId = continuation.sourceId,
                opponentId = opponents.firstOrNull(),
                targets = emptyList()
            )
            val effectResult = ctx.effectExecutorRegistry.execute(
                newState, continuation.abilityEffect, effectContext
            )
            if (effectResult.isSuccess) {
                newState = effectResult.newState
                events.addAll(effectResult.events)
            }
        }

        // If player declined, check if there are other promptOnDraw abilities before drawing
        if (!activated) {
            val newDeclinedSourceIds = continuation.declinedSourceIds + continuation.sourceId

            if (continuation.isDrawStep) {
                val turnManager = TurnManager(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
                val otherPrompt = turnManager.checkPromptOnDraw(
                    newState, playerId, continuation.drawCount, isDrawStep = true,
                    declinedSourceIds = newDeclinedSourceIds
                )
                if (otherPrompt != null) {
                    return ExecutionResult.paused(
                        otherPrompt.state,
                        otherPrompt.pendingDecision!!,
                        events + otherPrompt.events
                    )
                }
            } else {
                val drawExecutor = DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
                val otherPrompt = drawExecutor.checkPromptOnDraw(
                    newState, playerId, continuation.drawCount, continuation.drawnCardsSoFar,
                    declinedSourceIds = newDeclinedSourceIds
                )
                if (otherPrompt != null) {
                    return ExecutionResult.paused(
                        otherPrompt.state,
                        otherPrompt.pendingDecision!!,
                        events + otherPrompt.events
                    )
                }
            }
        }

        // Now perform the draws
        if (continuation.isDrawStep) {
            val turnManager = TurnManager(effectExecutor = ctx.effectExecutorRegistry::execute)
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
            val drawExecutor = DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
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
            // Player declined all abilities - draw 1 card normally,
            // then continue remaining draws with prompting enabled
            val singleDrawExecutor = DrawCardsExecutor(effectExecutor = ctx.effectExecutorRegistry::execute)
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

            // Continue remaining draws with prompting (reset declined list for next draw)
            val remainingDraws = continuation.drawCount - 1
            if (remainingDraws > 0) {
                val drawExecutor = DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after target selection for a "prompt on draw" ability that requires targeting
     * (e.g., Words of War). Creates the replacement shield with the chosen targets,
     * then proceeds with draws.
     */
    fun resumeDrawReplacementTarget(
        state: GameState,
        continuation: DrawReplacementTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for draw replacement target")
        }

        val playerId = continuation.drawingPlayerId
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Convert selected targets to ChosenTargets
        val chosenTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { targetId ->
                this.entityIdToChosenTarget(newState, targetId)
            }
        }

        // Execute the effect to create a replacement shield with targets
        val opponents = newState.turnOrder.filter { it != playerId }
        val effectContext = EffectContext(
            controllerId = playerId,
            sourceId = continuation.sourceId,
            opponentId = opponents.firstOrNull(),
            targets = chosenTargets
        )
        val effectResult = ctx.effectExecutorRegistry.execute(
            newState, continuation.abilityEffect, effectContext
        )
        if (effectResult.isSuccess) {
            newState = effectResult.newState
            events.addAll(effectResult.events)
        }

        // Now perform the draws (same logic as resumeDrawReplacementActivation)
        if (continuation.isDrawStep) {
            val turnManager = TurnManager(effectExecutor = ctx.effectExecutorRegistry::execute)
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
            newState = newState.withPriority(playerId)
        } else {
            // Spell/ability draws - use DrawCardsExecutor with cardRegistry for subsequent prompts
            val drawExecutor = DrawCardsExecutor(cardRegistry = ctx.stackResolver.cardRegistry, effectExecutor = ctx.effectExecutorRegistry::execute)
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
        }

        return checkForMore(newState, events)
    }
}
