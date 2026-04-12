package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

class ModalAndCloneContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ModalContinuation::class, ::resumeModal),
        resumer(ModalTargetContinuation::class, ::resumeModalTarget),
        resumer(CloneEntersContinuation::class, ::resumeCloneEnters),
        resumer(EntersWithChoiceSpellContinuation::class, ::resumeEntersWithChoiceSpell),
        resumer(EntersWithChoiceLandContinuation::class, ::resumeEntersWithChoiceLand),
        resumer(PayLifeOrEnterTappedLandContinuation::class, ::resumePayLifeOrEnterTappedLand),
        resumer(PayLifeOrEnterTappedSpellContinuation::class, ::resumePayLifeOrEnterTappedSpell),
        resumer(RevealCountersContinuation::class, ::resumeRevealCounters),
        resumer(CastWithCreatureTypeContinuation::class, ::resumeCastWithCreatureType),
        resumer(BudgetModalContinuation::class, ::resumeBudgetModal),
        resumer(CreateTokenCopyOfChosenContinuation::class, ::resumeCreateTokenCopyOfChosen),
        resumer(ChooseActionContinuation::class, ::resumeChooseAction)
    )

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
                val legalTargets = services.targetFinder.findLegalTargets(
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
                        pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(chosenMode.targetRequirements, chosenTargets))
                    )
                    val result = services.effectExecutorRegistry.execute(state, chosenMode.effect, context).toExecutionResult()
                    if (result.isPaused) return result
                    return checkForMore(result.state, result.events.toList())
                }
            }

            // Create target selection decision (with cancel support to go back to mode selection)
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
                legalTargets = legalTargetsMap,
                canCancel = true
            )

            val modalTargetContinuation = ModalTargetContinuation(
                decisionId = decisionId,
                controllerId = continuation.controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                effect = chosenMode.effect,
                xValue = continuation.xValue,
                opponentId = continuation.opponentId,
                targetRequirements = chosenMode.targetRequirements,
                modes = continuation.modes,
                triggeringEntityId = continuation.triggeringEntityId
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

        val result = services.effectExecutorRegistry.execute(state, chosenMode.effect, context).toExecutionResult()
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
        // Handle cancel: go back to mode selection
        if (response is CancelDecisionResponse && continuation.modes != null) {
            return revertToModeSelection(state, continuation)
        }

        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for modal spell")
        }

        // Convert selected targets to ChosenTargets, sorted by requirement index
        // so buildNamedTargets maps them correctly to each requirement
        val chosenTargets = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, targetIds) ->
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
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(continuation.targetRequirements, chosenTargets))
        )

        val result = services.effectExecutorRegistry.execute(state, continuation.effect, context).toExecutionResult()
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
                // Apply additional subtypes and keywords if specified (e.g., Mockingbird adds Bird + flying)
                var copiedCardComponent = targetCardComponent.copy(
                    ownerId = ownerId
                )
                if (continuation.additionalSubtypes.isNotEmpty()) {
                    val newSubtypes = copiedCardComponent.typeLine.subtypes +
                        continuation.additionalSubtypes.map { com.wingedsheep.sdk.core.Subtype(it) }
                    copiedCardComponent = copiedCardComponent.copy(
                        typeLine = copiedCardComponent.typeLine.copy(subtypes = newSubtypes)
                    )
                }
                if (continuation.additionalKeywords.isNotEmpty()) {
                    copiedCardComponent = copiedCardComponent.copy(
                        baseKeywords = copiedCardComponent.baseKeywords + continuation.additionalKeywords
                    )
                }

                // Update entity with copied card component and copy tracking
                newState = newState.updateEntity(spellId) { c ->
                    c.with(copiedCardComponent)
                        .with(com.wingedsheep.engine.state.components.identity.CopyOfComponent(
                            originalCardDefinitionId = originalCardComponent.cardDefinitionId,
                            copiedCardDefinitionId = targetCardComponent.cardDefinitionId
                        ))
                }

                // Look up the card definition for the copied creature
                copiedCardDef = services.cardRegistry.getCard(targetCardComponent.cardDefinitionId)
            } else {
                // Target creature no longer exists - enter as itself
                copiedCardDef = services.cardRegistry.getCard(originalCardComponent.cardDefinitionId)
            }
        } else {
            // Player declined to copy - enter as itself (0/0 Clone)
            copiedCardDef = services.cardRegistry.getCard(originalCardComponent.cardDefinitionId)
        }

        // Get the (possibly updated) card component for event names
        val finalCardComponent = newState.getEntity(spellId)?.get<CardComponent>() ?: originalCardComponent

        // Track whether a copy was made (original name differs from final name)
        val copyOfOriginalName = if (selectedCreatureId != null && finalCardComponent.name != originalCardComponent.name) {
            originalCardComponent.name
        } else null

        // Complete the permanent entry using the shared helper
        val (enterState, enterEvents) = services.stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, finalCardComponent, copiedCardDef
        )
        newState = enterState
        events.addAll(enterEvents)

        events.add(ResolvedEvent(spellId, finalCardComponent.name))
        events.add(
            ZoneChangeEvent(
                spellId,
                finalCardComponent.name,
                null,
                Zone.BATTLEFIELD,
                ownerId,
                copyOfOriginalName = copyOfOriginalName
            )
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after player makes an "as enters" choice for a spell being resolved.
     * Dispatches on choiceType to store the right component, then checks for chained
     * choices before completing the permanent entry.
     */
    fun resumeEntersWithChoiceSpell(
        state: GameState,
        continuation: EntersWithChoiceSpellContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId

        // Store the chosen value based on choice type
        var newState = when (continuation.choiceType) {
            com.wingedsheep.sdk.scripting.ChoiceType.COLOR -> {
                if (response !is ColorChosenResponse) {
                    return ExecutionResult.error(state, "Expected color choice response")
                }
                state.updateEntity(spellId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ChosenColorComponent(response.color))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for creature type choice")
                }
                val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                if (response !is CardsSelectedResponse) {
                    return ExecutionResult.error(state, "Expected cards selected response for creature choice")
                }
                val chosenCreatureId = response.selectedCards.firstOrNull()
                    ?: return ExecutionResult.error(state, "No creature selected")
                state.updateEntity(spellId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ChosenCreatureComponent(chosenCreatureId))
                }
            }
        }

        // Check if the card has remaining choices to chain to
        val spellContainer = newState.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")
        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")
        val cardDef = services.cardRegistry.getCard(cardComponent.cardDefinitionId)

        val nextChoice = cardDef?.script?.replacementEffects
            ?.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithChoice>()
            ?.sortedBy { it.choiceType.ordinal }
            ?.firstOrNull { it.choiceType.ordinal > continuation.choiceType.ordinal }

        if (nextChoice != null) {
            val result = services.stackResolver.pauseForEntersWithChoice(
                newState, spellId, controllerId, ownerId, cardComponent, nextChoice
            )
            if (result != null) return result
        }

        // No more choices — complete the permanent entry
        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        val (enterState, enterEvents) = services.stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, cardComponent, cardDef
        )
        newState = enterState

        val events = mutableListOf<GameEvent>()
        events.addAll(enterEvents)
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
     * Resume after player makes an "as enters" choice for a land played directly to the battlefield.
     * The land is already on the battlefield — just store the chosen value and check for chained choices.
     */
    fun resumeEntersWithChoiceLand(
        state: GameState,
        continuation: EntersWithChoiceLandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        // Store the chosen value based on choice type
        var newState = when (continuation.choiceType) {
            com.wingedsheep.sdk.scripting.ChoiceType.COLOR -> {
                if (response !is ColorChosenResponse) {
                    return ExecutionResult.error(state, "Expected color choice response")
                }
                state.updateEntity(continuation.landId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ChosenColorComponent(response.color))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for creature type choice")
                }
                val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")
                state.updateEntity(continuation.landId) { c ->
                    c.with(com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                // Lands don't use this choice type, but handle gracefully
                return checkForMore(state, emptyList())
            }
        }

        // Check if the land has remaining choices to chain to
        val landContainer = newState.getEntity(continuation.landId)
        val cardComponent = landContainer?.get<CardComponent>()
        val cardDef = cardComponent?.let { services.cardRegistry.getCard(it.cardDefinitionId) }

        val nextChoice = cardDef?.script?.replacementEffects
            ?.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithChoice>()
            ?.sortedBy { it.choiceType.ordinal }
            ?.firstOrNull { it.choiceType.ordinal > continuation.choiceType.ordinal }

        if (nextChoice != null) {
            val chooserId = when (nextChoice.chooser) {
                com.wingedsheep.sdk.scripting.references.Player.Opponent ->
                    newState.turnOrder.firstOrNull { it != continuation.controllerId } ?: continuation.controllerId
                else -> continuation.controllerId
            }

            when (nextChoice.choiceType) {
                com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE -> {
                    val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                    val decisionId = "choose-creature-type-land-enters-${continuation.landId.value}"
                    val decision = ChooseOptionDecision(
                        id = decisionId,
                        playerId = chooserId,
                        prompt = "Choose a creature type",
                        context = DecisionContext(
                            sourceId = continuation.landId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = allCreatureTypes,
                        defaultSearch = ""
                    )
                    val nextContinuation = EntersWithChoiceLandContinuation(
                        decisionId = decisionId,
                        landId = continuation.landId,
                        controllerId = continuation.controllerId,
                        choiceType = com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE,
                        creatureTypes = allCreatureTypes
                    )
                    val pausedState = newState
                        .pushContinuation(nextContinuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                else -> { /* No other chaining needed for lands */ }
            }
        }

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after player answers yes/no to "pay life or enter tapped" for a land played directly.
     *
     * The land is already on the battlefield. If yes -> pay life, land stays untapped.
     * If no -> land gets tapped. Then detect and process triggers from the land entering.
     */
    fun resumePayLifeOrEnterTappedLand(
        state: GameState,
        continuation: PayLifeOrEnterTappedLandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay life or enter tapped")
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        if (response.choice) {
            // Player chose to pay life
            val currentLife = newState.getEntity(continuation.controllerId)
                ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life
                ?: return ExecutionResult.error(state, "Player has no life total")
            val newLife = currentLife - continuation.lifeCost
            newState = newState.updateEntity(continuation.controllerId) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
            }
            newState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(
                newState, continuation.controllerId
            )
            events.add(LifeChangedEvent(continuation.controllerId, currentLife, newLife, LifeChangeReason.PAYMENT))
        } else {
            // Player chose not to pay — land enters tapped
            newState = newState.updateEntity(continuation.landId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
            }
        }

        // Detect and process any triggers from the land entering (e.g., landfall)
        val landContainer = newState.getEntity(continuation.landId)
        val cardComponent = landContainer?.get<CardComponent>()
        val zoneChangeEvent = ZoneChangeEvent(
            continuation.landId,
            cardComponent?.name ?: "Unknown",
            continuation.fromZone,
            Zone.BATTLEFIELD,
            continuation.controllerId
        )
        val triggerEvents = listOf(zoneChangeEvent)
        val triggers = services.triggerDetector.detectTriggers(newState, triggerEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = services.triggerProcessor.processTriggers(newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                events + triggerResult.events
            )
        }

        return checkForMore(newState, events)
    }

    /**
     * Resume after player answers yes/no to "pay life or enter tapped" for a spell resolving.
     *
     * If yes -> pay life, permanent enters untapped. If no -> permanent enters tapped.
     * Then completes the permanent entry to the battlefield.
     */
    fun resumePayLifeOrEnterTappedSpell(
        state: GameState,
        continuation: PayLifeOrEnterTappedSpellContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay life or enter tapped")
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        if (response.choice) {
            // Player chose to pay life
            val currentLife = newState.getEntity(continuation.controllerId)
                ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life
                ?: return ExecutionResult.error(state, "Player has no life total")
            val newLife = currentLife - continuation.lifeCost
            newState = newState.updateEntity(continuation.controllerId) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
            }
            newState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(
                newState, continuation.controllerId
            )
            events.add(LifeChangedEvent(continuation.controllerId, currentLife, newLife, LifeChangeReason.PAYMENT))
        }

        // Complete the permanent entry
        val spellContainer = newState.getEntity(continuation.spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: ${continuation.spellId}")

        val cardComponent = spellContainer.get<CardComponent>()
        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        val cardDef = cardComponent?.let { services.cardRegistry.getCard(it.cardDefinitionId) }
        val (enterState, enterEvents) = services.stackResolver.enterPermanentOnBattlefield(
            newState, continuation.spellId, spellComponent, cardComponent, cardDef
        )
        newState = enterState
        events.addAll(enterEvents)

        // If player declined to pay life, tap the permanent
        if (!response.choice) {
            newState = newState.updateEntity(continuation.spellId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
            }
        }

        events.add(ResolvedEvent(continuation.spellId, cardComponent?.name ?: "Unknown"))
        events.add(
            ZoneChangeEvent(
                continuation.spellId,
                cardComponent?.name ?: "Unknown",
                null,
                Zone.BATTLEFIELD,
                continuation.ownerId
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
        val castResult = services.stackResolver.castSpell(
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
        val triggers = services.triggerDetector.detectTriggers(castResult.newState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = services.triggerProcessor.processTriggers(castResult.newState, triggers)

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

        return ExecutionResult.success(
            castResult.newState.withPriority(continuation.casterId),
            allEvents
        )
    }

    /**
     * Resume after player reveals cards for enters-with-reveal-counters.
     *
     * Adds counters based on how many cards were revealed, then completes
     * the permanent entry to the battlefield.
     */
    fun resumeRevealCounters(
        state: GameState,
        continuation: RevealCountersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for reveal counters")
        }

        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId
        val revealedCards = response.selectedCards

        var newState = state

        // Add counters based on revealed cards
        val revealEvents = mutableListOf<GameEvent>()
        if (revealedCards.isNotEmpty()) {
            val resolvedCounterType = resolveCounterTypeFromString(continuation.counterType)
            if (resolvedCounterType != null) {
                val counterCount = revealedCards.size * continuation.countersPerReveal
                val current = newState.getEntity(spellId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                newState = newState.updateEntity(spellId) { c ->
                    c.with(current.withAdded(resolvedCounterType, counterCount))
                }
            }

            // Emit reveal event so opponent can see the revealed cards
            val cardNames = revealedCards.mapNotNull { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.name
            }
            val imageUris = revealedCards.mapNotNull { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val spellName = newState.getEntity(spellId)?.get<CardComponent>()?.name
            revealEvents.add(
                CardsRevealedEvent(
                    revealingPlayerId = controllerId,
                    cardIds = revealedCards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = spellName
                )
            )
        }

        // Complete the permanent entry
        val spellContainer = newState.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")

        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")

        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")

        val cardDef = services.cardRegistry.getCard(cardComponent.cardDefinitionId)
        val (enterState4, enterEvents4) = services.stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, cardComponent, cardDef
        )
        newState = enterState4

        val events = mutableListOf<GameEvent>()
        events.addAll(enterEvents4)
        events.addAll(revealEvents)
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

    private fun resolveCounterTypeFromString(counterType: String): com.wingedsheep.sdk.core.CounterType? {
        // Map string constants (from Counters object) to enum values
        val byDescription = com.wingedsheep.sdk.core.CounterType.entries.associateBy { entry ->
            entry.name.lowercase().replace('_', ' ')
        }
        return when (counterType) {
            "+1/+1" -> com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            "-1/-1" -> com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE
            else -> byDescription[counterType.lowercase()] ?: run {
                System.err.println("WARNING: Unknown counter type '$counterType' in EntersWithRevealCounters, skipping counter placement")
                null
            }
        }
    }

    /**
     * Resume after player chose budget modal modes (Season cycle pawprints).
     * Receives a BudgetModalResponse with all selected mode indices at once.
     * Validates the total cost fits the budget, then executes in printed order.
     */
    fun resumeBudgetModal(
        state: GameState,
        continuation: BudgetModalContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is BudgetModalResponse) {
            return ExecutionResult.error(state, "Expected budget modal response")
        }

        val selectedIndices = response.selectedModeIndices

        // Validate all indices are in range
        for (idx in selectedIndices) {
            if (idx < 0 || idx >= continuation.modes.size) {
                return ExecutionResult.error(state, "Invalid mode index: $idx")
            }
        }

        // Validate total cost doesn't exceed budget
        val totalCost = selectedIndices.sumOf { continuation.modes[it].cost }
        if (totalCost > continuation.remainingBudget) {
            return ExecutionResult.error(state, "Total cost $totalCost exceeds budget ${continuation.remainingBudget}")
        }

        // No modes chosen → no-op
        if (selectedIndices.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        // Sort by mode index for printed-order execution
        val sortedIndices = selectedIndices.sorted()

        val effects = sortedIndices.map { continuation.modes[it].effect }

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = continuation.opponentId
        )

        val result = services.effectExecutorRegistry.execute(state, CompositeEffect(effects), context).toExecutionResult()
        if (result.isPaused) return result
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Resume after player chose a permanent to create a token copy of.
     */
    fun resumeCreateTokenCopyOfChosen(
        state: GameState,
        continuation: CreateTokenCopyOfChosenContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for token copy")
        }

        if (response.selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val chosenId = response.selectedCards.first()
        val staticAbilityHandler = com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler(services.cardRegistry)
        val result = com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfChosenPermanentExecutor.createTokenCopy(
            state, chosenId, continuation.controllerId,
            staticAbilityHandler
        ).toExecutionResult()
        if (result.isPaused) return result
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Revert from target selection back to mode selection for a modal spell.
     */
    private fun revertToModeSelection(
        state: GameState,
        continuation: ModalTargetContinuation
    ): ExecutionResult {
        val modes = continuation.modes!!
        val sourceName = continuation.sourceName ?: "modal spell"

        val modeDescriptions = modes.map { it.description }
        val decisionId = java.util.UUID.randomUUID().toString()

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Choose a mode for $sourceName",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = modeDescriptions
        )

        val modalContinuation = ModalContinuation(
            decisionId = decisionId,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            modes = modes,
            xValue = continuation.xValue,
            opponentId = continuation.opponentId,
            triggeringEntityId = continuation.triggeringEntityId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(modalContinuation)

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

    // =========================================================================
    // ChooseAction
    // =========================================================================

    private fun resumeChooseAction(
        state: GameState,
        continuation: ChooseActionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for ChooseActionEffect")
        }

        val choiceIndex = response.optionIndex
        if (choiceIndex < 0 || choiceIndex >= continuation.choices.size) {
            return ExecutionResult.error(state, "Invalid choice index: $choiceIndex")
        }

        val chosenEffect = continuation.choices[choiceIndex].effect

        // Build context preserving original targets so ContextTarget references still work
        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = continuation.opponentId,
            targets = continuation.targets,
            triggeringEntityId = continuation.triggeringEntityId,
            pipeline = PipelineState(namedTargets = continuation.namedTargets)
        )

        val result = services.effectExecutorRegistry.execute(state, chosenEffect, context).toExecutionResult()

        return if (result.isPaused) {
            result
        } else {
            checkForMore(result.state, result.events.toList())
        }
    }

}
