package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Mode
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
        resumer(EntersWithChoiceOnBattlefieldContinuation::class, ::resumeEntersWithChoiceOnBattlefield),
        resumer(PayLifeOrEnterTappedLandContinuation::class, ::resumePayLifeOrEnterTappedLand),
        resumer(PayLifeOrEnterTappedSpellContinuation::class, ::resumePayLifeOrEnterTappedSpell),
        resumer(RevealCountersContinuation::class, ::resumeRevealCounters),
        resumer(DevourEntersContinuation::class, ::resumeDevourEnters),
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

        val availableIndices = continuation.availableIndices ?: continuation.modes.indices.toList()
        val optionIndex = response.optionIndex
        // "Choose up to N" — a synthetic decline option is appended after the
        // mode options when minChooseCount is satisfied. Picking it short-circuits
        // execution with no chosen modes.
        val canDecline = continuation.selectedModeIndices.size >= continuation.minChooseCount &&
            continuation.selectedModeIndices.size < continuation.chooseCount
        val declineIndex = if (canDecline) availableIndices.size else -1
        val maxIndex = if (canDecline) availableIndices.size else availableIndices.size - 1
        if (optionIndex < 0 || optionIndex > maxIndex) {
            return ExecutionResult.error(state, "Invalid mode option index: $optionIndex")
        }
        if (optionIndex == declineIndex) {
            return resolveChosenModes(state, continuation, continuation.selectedModeIndices, checkForMore)
        }
        val originalModeIndex = availableIndices[optionIndex]
        val newSelectedIndices = continuation.selectedModeIndices + originalModeIndex
        val newAvailableIndices = availableIndices.toMutableList().also { it.removeAt(optionIndex) }

        // "Choose one that hasn't been chosen" (Gandalf the Grey): record the chosen mode
        // on the source so later triggers exclude it. Persists for the source's lifetime.
        val stateAfterRecord = if (continuation.recordChosenModesOnSource && continuation.sourceId != null) {
            recordChosenMode(state, continuation.sourceId, originalModeIndex)
        } else state

        // More modes still need to be picked — present the next ChooseOptionDecision.
        if (newSelectedIndices.size < continuation.chooseCount && newAvailableIndices.isNotEmpty()) {
            val sourceName = continuation.sourceName ?: "modal spell"
            val decisionId = java.util.UUID.randomUUID().toString()
            val prompt = "Choose a mode for $sourceName (${newSelectedIndices.size + 1} of ${continuation.chooseCount})"
            val nextCanDecline = newSelectedIndices.size >= continuation.minChooseCount
            val baseOptions = newAvailableIndices.map { continuation.modes[it].description }
            val decisionOptions = if (nextCanDecline) {
                baseOptions + com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor.DECLINE_MODE_LABEL
            } else baseOptions
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = continuation.controllerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = decisionOptions
            )
            val nextContinuation = continuation.copy(
                decisionId = decisionId,
                selectedModeIndices = newSelectedIndices,
                availableIndices = newAvailableIndices
            )
            val stateWithDecision = stateAfterRecord.withPendingDecision(decision)
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

        return resolveChosenModes(stateAfterRecord, continuation, newSelectedIndices, checkForMore)
    }

    /**
     * Record a chosen mode index on the source's
     * [com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent]
     * for "choose one that hasn't been chosen" effects (Gandalf the Grey).
     */
    private fun recordChosenMode(state: GameState, sourceId: EntityId, modeIndex: Int): GameState {
        if (state.getEntity(sourceId) == null) return state
        return state.updateEntity(sourceId) { c ->
            val existing = c.get<com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent()
            c.with(existing.withChosen(modeIndex))
        }
    }

    /**
     * Execute the modes selected so far, in pick order. Used both when the player
     * has met `chooseCount` and when they decline a "choose up to N" pick.
     */
    private fun resolveChosenModes(
        state: GameState,
        continuation: ModalContinuation,
        selectedModeIndices: List<Int>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val chosenModes = selectedModeIndices.map { continuation.modes[it] }
        // For single-mode flow, preserve revert-to-mode-selection by passing the full modes list
        // when there's only one chosen mode and it needs targets. For multi-mode, don't allow cancel.
        val allowCancelBackToModes = continuation.chooseCount == 1 && chosenModes.size == 1
        return processChosenModeQueue(
            services = services,
            state = state,
            queue = chosenModes,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            xValue = continuation.xValue,
            triggeringEntityId = continuation.triggeringEntityId,
            allowCancelBackToModesList = if (allowCancelBackToModes) continuation.modes else null,
            outerTargets = continuation.outerTargets,
            outerNamedTargets = continuation.outerNamedTargets,
            accumulatedEvents = emptyList(),
            checkForMore = checkForMore
        )
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
        // Handle cancel: go back to mode selection (single-mode flow only)
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
            xValue = continuation.xValue,
            targets = chosenTargets,
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(continuation.targetRequirements, chosenTargets)),
            triggeringEntityId = continuation.triggeringEntityId
        )

        // Execute this mode's effect with the picked targets. The helper pre-pushes a
        // tail frame carrying any remaining chosen modes so that if this effect itself
        // pauses (e.g. a targeted Scry's reorder decision), the rest of the queue
        // (choose-N modal) still resumes afterward instead of being dropped.
        return executeChosenModeWithTail(
            services = services,
            state = state,
            effect = continuation.effect,
            context = context,
            tail = continuation.remainingChosenModes,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            xValue = continuation.xValue,
            triggeringEntityId = continuation.triggeringEntityId,
            outerTargets = continuation.outerTargets,
            outerNamedTargets = continuation.outerNamedTargets,
            accumulatedEvents = emptyList(),
            checkForMore = checkForMore
        )
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
        var copyApplied = false

        if (selectedCreatureId != null) {
            val targetContainer = newState.getEntity(selectedCreatureId)
            val targetCardComponent = targetContainer?.get<CardComponent>()

            if (targetCardComponent != null) {
                copyApplied = true
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
                // Name override (e.g., Superior Spider-Man keeps his own name)
                if (continuation.nameOverride != null) {
                    copiedCardComponent = copiedCardComponent.copy(name = continuation.nameOverride)
                }
                // Power/toughness override (e.g., Superior Spider-Man is always 4/4)
                if (continuation.powerOverride != null || continuation.toughnessOverride != null) {
                    val basePower = continuation.powerOverride
                        ?: copiedCardComponent.baseStats?.basePower ?: 0
                    val baseToughness = continuation.toughnessOverride
                        ?: copiedCardComponent.baseStats?.baseToughness ?: 0
                    copiedCardComponent = copiedCardComponent.copy(
                        baseStats = com.wingedsheep.sdk.model.CreatureStats(basePower, baseToughness)
                    )
                }

                // Update entity with copied card component and copy tracking.
                // Snapshot the pre-copy CardComponent so the permanent reverts to its
                // printed identity when it leaves the battlefield (CR 400.7 / 707.2).
                newState = newState.updateEntity(spellId) { c ->
                    c.with(copiedCardComponent)
                        .with(com.wingedsheep.engine.state.components.identity.CopyOfComponent(
                            originalCardDefinitionId = originalCardComponent.cardDefinitionId,
                            copiedCardDefinitionId = targetCardComponent.cardDefinitionId,
                            originalCardComponent = originalCardComponent
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

        // "When you do, exile that card." (Superior Spider-Man) — exile the copied
        // graveyard card after the copy has been applied and the permanent has entered.
        if (continuation.exileCopiedCard && copyApplied && selectedCreatureId != null) {
            val exileResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .moveToZone(newState, selectedCreatureId, Zone.EXILE)
            newState = exileResult.state
            events.addAll(exileResult.events)
        }

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

        // Store the chosen value based on choice type — into the unified cast-choices bag.
        var newState = when (continuation.choiceType) {
            com.wingedsheep.sdk.scripting.ChoiceType.COLOR -> {
                if (response !is ColorChosenResponse) {
                    return ExecutionResult.error(state, "Expected color choice response")
                }
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.COLOR, ChoiceValue.ColorChoice(response.color))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for creature type choice")
                }
                val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.CREATURE_TYPE, ChoiceValue.TextChoice(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                if (response !is CardsSelectedResponse) {
                    return ExecutionResult.error(state, "Expected cards selected response for creature choice")
                }
                val chosenCreatureId = response.selectedCards.firstOrNull()
                    ?: return ExecutionResult.error(state, "No creature selected")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.CREATURE, ChoiceValue.EntityChoice(chosenCreatureId))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.MODE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for mode choice")
                }
                val modeId = continuation.modeOptionIds.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid mode option index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.MODE, ChoiceValue.TextChoice(modeId))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.BASIC_LAND_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for land type choice")
                }
                val chosenType = continuation.landTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid land type index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.LAND_TYPE, ChoiceValue.TextChoice(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.OPPONENT -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for opponent choice")
                }
                val chosenOpponent = continuation.opponentIds.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid opponent index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.OPPONENT, ChoiceValue.EntityChoice(chosenOpponent))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CARD_NAME -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for card name choice")
                }
                val chosenName = continuation.cardNames.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid card name index: ${response.optionIndex}")
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice(chosenName))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.NUMBER -> {
                if (response !is NumberChosenResponse) {
                    return ExecutionResult.error(state, "Expected number chosen response for number choice")
                }
                state.updateEntity(spellId) { c ->
                    c.withCastChoice(ChoiceSlot.CHOSEN_NUMBER, ChoiceValue.NumberChoice(response.number))
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
    fun resumeEntersWithChoiceOnBattlefield(
        state: GameState,
        continuation: EntersWithChoiceOnBattlefieldContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val entityId = continuation.entityId
        // Store the chosen value based on choice type — into the unified cast-choices bag.
        var newState = when (continuation.choiceType) {
            com.wingedsheep.sdk.scripting.ChoiceType.COLOR -> {
                if (response !is ColorChosenResponse) {
                    return ExecutionResult.error(state, "Expected color choice response")
                }
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.COLOR, ChoiceValue.ColorChoice(response.color))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for creature type choice")
                }
                val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.CREATURE_TYPE, ChoiceValue.TextChoice(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                if (response !is CardsSelectedResponse) {
                    return ExecutionResult.error(state, "Expected cards selected response for creature choice")
                }
                val chosenCreatureId = response.selectedCards.firstOrNull()
                    ?: return ExecutionResult.error(state, "No creature selected")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.CREATURE, ChoiceValue.EntityChoice(chosenCreatureId))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.MODE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for mode choice")
                }
                val modeId = continuation.modeOptionIds.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid mode option index: ${response.optionIndex}")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.MODE, ChoiceValue.TextChoice(modeId))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.BASIC_LAND_TYPE -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for land type choice")
                }
                val chosenType = continuation.landTypes.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid land type index: ${response.optionIndex}")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.LAND_TYPE, ChoiceValue.TextChoice(chosenType))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.OPPONENT -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for opponent choice")
                }
                val chosenOpponent = continuation.opponentIds.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid opponent index: ${response.optionIndex}")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.OPPONENT, ChoiceValue.EntityChoice(chosenOpponent))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.CARD_NAME -> {
                if (response !is OptionChosenResponse) {
                    return ExecutionResult.error(state, "Expected option chosen response for card name choice")
                }
                val chosenName = continuation.cardNames.getOrNull(response.optionIndex)
                    ?: return ExecutionResult.error(state, "Invalid card name index: ${response.optionIndex}")
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice(chosenName))
                }
            }
            com.wingedsheep.sdk.scripting.ChoiceType.NUMBER -> {
                if (response !is NumberChosenResponse) {
                    return ExecutionResult.error(state, "Expected number chosen response for number choice")
                }
                state.updateEntity(entityId) { c ->
                    c.withCastChoice(ChoiceSlot.CHOSEN_NUMBER, ChoiceValue.NumberChoice(response.number))
                }
            }
        }

        // Check if the permanent has remaining choices to chain to (e.g. color + creature type).
        val entityContainer = newState.getEntity(entityId)
        val cardComponent = entityContainer?.get<CardComponent>()
        val cardDef = cardComponent?.let { services.cardRegistry.getCard(it.cardDefinitionId) }

        val nextChoice = cardDef?.script?.replacementEffects
            ?.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithChoice>()
            ?.sortedBy { it.choiceType.ordinal }
            ?.firstOrNull { it.choiceType.ordinal > continuation.choiceType.ordinal }

        if (nextChoice != null) {
            val result = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements.pauseForEntersWithChoice(
                newState, entityId, continuation.controllerId, cardComponent, nextChoice, continuation.fromZone
            )
            if (result != null) return result
            // null means the chained choice couldn't be presented — fall through to fire triggers.
        }

        // Final choice resolved — fire any triggers from the permanent entering (e.g. landfall,
        // "when ~ enters"). The permanent already moved to the battlefield when it was placed; we
        // synthesize the matching ZoneChangeEvent here so triggers can react now that the chosen
        // value is recorded.
        val zoneChangeEvent = ZoneChangeEvent(
            entityId,
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
                    triggerResult.events
                )
            }
            return checkForMore(triggerResult.newState, triggerResult.events)
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
            if (newState.getEntity(continuation.controllerId)
                    ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>() == null
            ) return ExecutionResult.error(state, "Player has no life total")
            // CR 810.9a — life paid as a cost comes out of the team's shared total.
            val currentLife = newState.lifeTotal(continuation.controllerId)
            val newLife = currentLife - continuation.lifeCost
            newState = newState.withLifeTotal(continuation.controllerId, newLife)
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
            if (newState.getEntity(continuation.controllerId)
                    ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>() == null
            ) return ExecutionResult.error(state, "Player has no life total")
            // CR 810.9a — life paid as a cost comes out of the team's shared total.
            val currentLife = newState.lifeTotal(continuation.controllerId)
            val newLife = currentLife - continuation.lifeCost
            newState = newState.withLifeTotal(continuation.controllerId, newLife)
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

    /**
     * Resume after the player selects permanents to sacrifice for Devour.
     *
     * Sacrifices the chosen permanents (Rule 701.21 — owner of each goes to graveyard),
     * places `multiplier × count` counters of the saved counter type on the entering
     * spell entity, then completes the permanent entry to the battlefield.
     */
    fun resumeDevourEnters(
        state: GameState,
        continuation: DevourEntersContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for devour")
        }

        val spellId = continuation.spellId
        val controllerId = continuation.controllerId
        val ownerId = continuation.ownerId
        val sacrificed = response.selectedCards

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Sacrifice the chosen permanents.
        if (sacrificed.isNotEmpty()) {
            val names = sacrificed.map { id ->
                newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(
                com.wingedsheep.engine.core.PermanentsSacrificedEvent(
                    controllerId, sacrificed, names
                )
            )
            newState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .trackPermanentSacrifice(newState, sacrificed, controllerId)
            for (permanentId in sacrificed) {
                val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .moveToZone(newState, permanentId, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
                newState = result.state
                events.addAll(result.events)
            }
        }

        // Place counters on the still-resolving spell entity.
        val counterCount = sacrificed.size * continuation.multiplier
        if (counterCount > 0) {
            val resolvedCounterType = resolveCounterTypeFromString(continuation.counterType)
                ?: com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            val current = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
            newState = newState.updateEntity(spellId) { c ->
                c.with(current.withAdded(resolvedCounterType, counterCount))
            }
            val (afterMark, firstThisTurn) = com.wingedsheep.engine.handlers.effects.DamageUtils
                .recordCounterPlacement(newState, spellId)
            newState = afterMark
            val spellName = newState.getEntity(spellId)?.get<CardComponent>()?.name ?: ""
            events.add(
                com.wingedsheep.engine.core.CountersAddedEvent(
                    spellId, continuation.counterType, counterCount, spellName, firstThisTurn
                )
            )
        }

        // Complete the permanent entry.
        val spellContainer = newState.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")
        val cardComponent = spellContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no CardComponent")
        val spellComponent = spellContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")
        val cardDef = services.cardRegistry.getCard(cardComponent.cardDefinitionId)

        val (afterEnter, enterEvents) = services.stackResolver.enterPermanentOnBattlefield(
            newState, spellId, spellComponent, cardComponent, cardDef
        )
        newState = afterEnter
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
            triggeringEntityId = continuation.triggeringEntityId,
            outerTargets = continuation.outerTargets,
            outerNamedTargets = continuation.outerNamedTargets
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

/**
 * Sequentially resolve a queue of chosen modal-ability modes (resolution-time modal
 * triggered / activated abilities — rule 603.3c).
 *
 * - Modes that need no targets execute immediately; their events accumulate.
 * - A mode requiring targets auto-selects a lone player target, silently fizzles when
 *   all its targets have become illegal (Rule 608.2b), or pauses with a
 *   [ChooseTargetsDecision] otherwise; the remaining modes ride along on the
 *   [ModalTargetContinuation].
 * - **Every** mode is executed through [executeChosenModeWithTail], which pushes a
 *   [ModalChosenModeTailContinuation] beneath the mode's effect first, so a *nested*
 *   pause inside a mode (a targeted Scry's reorder prompt, a ChooseAction, …) resumes
 *   the rest of the queue afterward instead of dropping it.
 *
 * Top-level so both the resumer and the [CoreAutoResumerModule] auto-resumer for
 * [ModalChosenModeTailContinuation] can drive it — the resolution-time twin of
 * [com.wingedsheep.engine.handlers.effects.composite.processPreChosenModeQueue].
 */
internal fun processChosenModeQueue(
    services: EngineServices,
    state: GameState,
    queue: List<Mode>,
    controllerId: EntityId,
    sourceId: EntityId?,
    sourceName: String?,
    xValue: Int?,
    triggeringEntityId: EntityId?,
    allowCancelBackToModesList: List<Mode>?,
    outerTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
    outerNamedTargets: Map<String, com.wingedsheep.engine.state.components.stack.ChosenTarget>,
    accumulatedEvents: List<GameEvent>,
    checkForMore: CheckForMore
): ExecutionResult {
    if (queue.isEmpty()) return checkForMore(state, accumulatedEvents)

    val head = queue.first()
    val tail = queue.drop(1)
    val displayName = sourceName ?: "modal spell"

    if (head.targetRequirements.isEmpty()) {
        // No targets — execute directly, inheriting outer-scope targets so
        // EffectTarget.ContextTarget references in the inner effect resolve to the
        // targets chosen by the enclosing spell/ability.
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            xValue = xValue,
            targets = outerTargets,
            pipeline = PipelineState(namedTargets = outerNamedTargets),
            triggeringEntityId = triggeringEntityId
        )
        return executeChosenModeWithTail(
            services, state, head.effect, context, tail,
            controllerId, sourceId, sourceName, xValue, triggeringEntityId,
            outerTargets, outerNamedTargets, accumulatedEvents, checkForMore
        )
    }

    // Targets required — find legal targets, auto-select / skip / pause as needed.
    val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
    val requirementInfos = head.targetRequirements.mapIndexed { index, req ->
        val legalTargets = services.targetFinder.findLegalTargets(
            state = state,
            requirement = req,
            controllerId = controllerId,
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

    val allSatisfied = requirementInfos.all { info ->
        (legalTargetsMap[info.index]?.isNotEmpty() == true) || info.minTargets == 0
    }
    if (!allSatisfied) {
        // Fizzle just this mode; continue with the rest.
        return processChosenModeQueue(
            services, state, tail, controllerId, sourceId, sourceName, xValue,
            triggeringEntityId, allowCancelBackToModesList, outerTargets, outerNamedTargets,
            accumulatedEvents, checkForMore
        )
    }

    // Auto-select single player target.
    if (head.targetRequirements.size == 1) {
        val req = head.targetRequirements[0]
        val targets = legalTargetsMap[0] ?: emptyList()
        val isPlayerTarget = req is TargetPlayer || req is TargetOpponent
        if (isPlayerTarget && targets.size == 1 && req.count == 1) {
            val chosenTargets = listOf(entityIdToChosenTarget(state, targets[0]))
            val context = EffectContext(
                sourceId = sourceId,
                controllerId = controllerId,
                xValue = xValue,
                targets = chosenTargets,
                pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(head.targetRequirements, chosenTargets)),
                triggeringEntityId = triggeringEntityId
            )
            return executeChosenModeWithTail(
                services, state, head.effect, context, tail,
                controllerId, sourceId, sourceName, xValue, triggeringEntityId,
                outerTargets, outerNamedTargets, accumulatedEvents, checkForMore
            )
        }
    }

    // Pause for target selection. Always surface the mode description so the player
    // knows which mode of a Choose-N modal ability they are targeting for. The tail
    // rides on the ModalTargetContinuation; resumeModalTarget re-enters via
    // executeChosenModeWithTail so a nested pause inside this mode still survives.
    val decisionId = java.util.UUID.randomUUID().toString()
    val prompt = "Choose targets for $displayName — ${head.description}"
    val decision = ChooseTargetsDecision(
        id = decisionId,
        playerId = controllerId,
        prompt = prompt,
        context = DecisionContext(
            sourceId = sourceId,
            sourceName = sourceName,
            phase = DecisionPhase.RESOLUTION
        ),
        targetRequirements = requirementInfos,
        legalTargets = legalTargetsMap,
        canCancel = allowCancelBackToModesList != null && tail.isEmpty()
    )

    val modalTargetContinuation = ModalTargetContinuation(
        decisionId = decisionId,
        controllerId = controllerId,
        sourceId = sourceId,
        sourceName = sourceName,
        effect = head.effect,
        xValue = xValue,
        targetRequirements = head.targetRequirements,
        modes = if (tail.isEmpty()) allowCancelBackToModesList else null,
        triggeringEntityId = triggeringEntityId,
        remainingChosenModes = tail,
        outerTargets = outerTargets,
        outerNamedTargets = outerNamedTargets
    )

    val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(modalTargetContinuation)

    return ExecutionResult.paused(
        stateWithContinuation,
        decision,
        accumulatedEvents + DecisionRequestedEvent(
            decisionId = decisionId,
            playerId = controllerId,
            decisionType = "CHOOSE_TARGETS",
            prompt = decision.prompt
        )
    )
}

/**
 * Execute one chosen mode's effect, keeping the rest of the choose-N queue alive across
 * a nested pause.
 *
 * Before running [effect], a [ModalChosenModeTailContinuation] carrying [tail] is pushed
 * so that if the effect pauses for its own decision, that decision's continuation lands
 * ON TOP and the tail frame sits beneath — the [CoreAutoResumerModule] auto-resumer then
 * drains [tail] once the inner chain finishes. On synchronous success the pre-pushed
 * frame is popped and the tail drains immediately. Mirrors
 * [com.wingedsheep.engine.handlers.effects.composite.processPreChosenModeQueue]'s
 * pre-push-tail discipline.
 */
private fun executeChosenModeWithTail(
    services: EngineServices,
    state: GameState,
    effect: com.wingedsheep.sdk.scripting.effects.Effect,
    context: EffectContext,
    tail: List<Mode>,
    controllerId: EntityId,
    sourceId: EntityId?,
    sourceName: String?,
    xValue: Int?,
    triggeringEntityId: EntityId?,
    outerTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
    outerNamedTargets: Map<String, com.wingedsheep.engine.state.components.stack.ChosenTarget>,
    accumulatedEvents: List<GameEvent>,
    checkForMore: CheckForMore
): ExecutionResult {
    val stateForExecution = if (tail.isNotEmpty()) {
        state.pushContinuation(
            ModalChosenModeTailContinuation(
                decisionId = "modal-chosen-tail-${java.util.UUID.randomUUID()}",
                controllerId = controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                xValue = xValue,
                triggeringEntityId = triggeringEntityId,
                remainingChosenModes = tail,
                outerTargets = outerTargets,
                outerNamedTargets = outerNamedTargets
            )
        )
    } else state

    val result = services.effectExecutorRegistry.execute(stateForExecution, effect, context).toExecutionResult()
    val events = accumulatedEvents + result.events

    if (result.isPaused) {
        return ExecutionResult.paused(result.state, result.pendingDecision!!, events)
    }
    if (result.error != null) {
        return result.copy(events = events)
    }

    // Success — pop the pre-pushed tail frame and drain the rest synchronously.
    val nextState = if (tail.isNotEmpty()) result.state.popContinuation().second else result.state
    return processChosenModeQueue(
        services, nextState, tail, controllerId, sourceId, sourceName, xValue,
        triggeringEntityId, allowCancelBackToModesList = null,
        outerTargets, outerNamedTargets, events, checkForMore
    )
}
