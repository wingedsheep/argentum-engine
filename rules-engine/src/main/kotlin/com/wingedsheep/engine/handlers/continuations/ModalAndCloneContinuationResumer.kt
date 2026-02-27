package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

class ModalAndCloneContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeModal(
        state: GameState,
        continuation: ModalContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
                val legalTargets = ctx.targetFinder.findLegalTargets(
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
                return checkForMore(state, emptyList())
            }

            // If single player-target requirement with exactly one valid target, auto-select
            if (chosenMode.targetRequirements.size == 1) {
                val req = chosenMode.targetRequirements[0]
                val targets = legalTargetsMap[0] ?: emptyList()
                val isPlayerTarget = req is TargetPlayer || req is TargetOpponent
                if (isPlayerTarget && targets.size == 1 && req.count == 1) {
                    // Auto-select the single target
                    val chosenTarget = entityIdToChosenTarget(state, targets[0])
                    val chosenTargets = listOf(chosenTarget)
                    val context = EffectContext(
                        sourceId = sourceId,
                        controllerId = continuation.controllerId,
                        opponentId = continuation.opponentId,
                        xValue = continuation.xValue,
                        targets = chosenTargets,
                        namedTargets = EffectContext.buildNamedTargets(chosenMode.targetRequirements, chosenTargets)
                    )
                    val result = ctx.effectExecutorRegistry.execute(state, chosenMode.effect, context)
                    if (result.isPaused) return result
                    return checkForMore(result.state, result.events.toList())
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
                opponentId = continuation.opponentId,
                targetRequirements = chosenMode.targetRequirements
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
            xValue = continuation.xValue,
            triggeringEntityId = continuation.triggeringEntityId
        )

        val result = ctx.effectExecutorRegistry.execute(state, chosenMode.effect, context)
        if (result.isPaused) return result
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Resume after player selected targets for a modal spell mode.
     * Execute the chosen mode's effect with the selected targets.
     */
    fun resumeModalTarget(
        state: GameState,
        continuation: ModalTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            targets = chosenTargets,
            namedTargets = EffectContext.buildNamedTargets(continuation.targetRequirements, chosenTargets)
        )

        val result = ctx.effectExecutorRegistry.execute(state, continuation.effect, context)
        if (result.isPaused) return result
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Resume after player selects a creature to copy for Clone-style effects.
     */
    fun resumeCloneEnters(
        state: GameState,
        continuation: CloneEntersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
                copiedCardDef = ctx.stackResolver.cardRegistry?.getCard(targetCardComponent.cardDefinitionId)
            } else {
                // Target creature no longer exists - enter as itself
                copiedCardDef = ctx.stackResolver.cardRegistry?.getCard(originalCardComponent.cardDefinitionId)
            }
        } else {
            // Player declined to copy - enter as itself (0/0 Clone)
            copiedCardDef = ctx.stackResolver.cardRegistry?.getCard(originalCardComponent.cardDefinitionId)
        }

        // Get the (possibly updated) card component for event names
        val finalCardComponent = newState.getEntity(spellId)?.get<CardComponent>() ?: originalCardComponent

        // Complete the permanent entry using the shared helper
        newState = ctx.stackResolver.enterPermanentOnBattlefield(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chooses a color for an "as enters, choose a color" effect (e.g., Riptide Replicator).
     * After storing the color, checks if the card also has EntersWithCreatureTypeChoice and chains to that.
     */
    fun resumeChooseColorEnters(
        state: GameState,
        continuation: ChooseColorEntersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
        val cardDef = ctx.stackResolver.cardRegistry?.getCard(cardComponent.cardDefinitionId)
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

        newState = ctx.stackResolver.enterPermanentOnBattlefield(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chooses a creature type for an "as enters" effect (e.g., Doom Cannon).
     */
    fun resumeChooseCreatureTypeEnters(
        state: GameState,
        continuation: ChooseCreatureTypeEntersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
        val cardDef = ctx.stackResolver.cardRegistry?.getCard(cardComponent.cardDefinitionId)
        newState = ctx.stackResolver.enterPermanentOnBattlefield(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume casting a spell after the player chose a creature type during casting.
     *
     * Completes the casting by putting the spell on the stack with the chosen type,
     * then detects and processes triggers (same as CastSpellHandler does).
     */
    fun resumeCastWithCreatureType(
        state: GameState,
        continuation: CastWithCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Complete casting: put spell on stack with the chosen creature type
        val castResult = ctx.stackResolver.castSpell(
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
        if (ctx.triggerDetector != null && ctx.triggerProcessor != null) {
            val triggers = ctx.triggerDetector.detectTriggers(castResult.newState, allEvents)
            if (triggers.isNotEmpty()) {
                val triggerResult = ctx.triggerProcessor.processTriggers(castResult.newState, triggers)

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
     * Resume after player reveals cards from hand for Amplify.
     *
     * Adds +1/+1 counters based on how many cards were revealed, then completes
     * the permanent entry to the battlefield.
     */
    fun resumeAmplifyEnters(
        state: GameState,
        continuation: AmplifyEntersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for Amplify")
        }

        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId
        val revealedCards = response.selectedCards

        var newState = state

        // Add +1/+1 counters based on revealed cards
        if (revealedCards.isNotEmpty()) {
            val counterCount = revealedCards.size * continuation.countersPerReveal
            val current = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
            newState = newState.updateEntity(spellId) { c ->
                c.with(current.withAdded(
                    com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE,
                    counterCount
                ))
            }
        }

        // Complete the permanent entry
        val spellContainer = newState.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")

        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")

        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        val cardDef = ctx.stackResolver.cardRegistry?.getCard(cardComponent.cardDefinitionId)
        newState = ctx.stackResolver.enterPermanentOnBattlefield(
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

        return checkForMore(newState, events)
    }

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
