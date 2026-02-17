package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

class CreatureTypeChoiceContinuationResumer(
    private val ctx: ContinuationContext
) {

    /**
     * Resume after player chose a creature type for graveyard retrieval.
     * Filters graveyard for creatures of that type and presents a card selection.
     */
    fun resumeChooseCreatureTypeReturn(
        state: GameState,
        continuation: ChooseCreatureTypeReturnContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            typeLine != null && typeLine.isCreature && typeLine.hasSubtype(Subtype(chosenType))
        }

        // If no matching cards, nothing to return
        if (matchingCards.isEmpty()) {
            return checkForMore(state, emptyList())
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
    fun resumeGraveyardToHand(
        state: GameState,
        continuation: GraveyardToHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chooses the FROM creature type for text replacement.
     * Presents the TO creature type choice (excluding Wall).
     */
    fun resumeChooseFromCreatureType(
        state: GameState,
        continuation: ChooseFromCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val fromType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Present TO creature type choice, excluding any types specified by the effect
        val excludedTypes = continuation.excludedTypes.map { it.lowercase() }.toSet()
        val toOptions = Subtype.ALL_CREATURE_TYPES.filter {
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
    fun resumeChooseToCreatureType(
        state: GameState,
        continuation: ChooseToCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val toType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still exist
        if (state.getEntity(targetId) == null) {
            return checkForMore(state, emptyList())
        }

        // Add or update TextReplacementComponent on the target
        val existingComponent = state.getEntity(targetId)
            ?.get<TextReplacementComponent>()

        val replacement = TextReplacement(
            fromWord = continuation.fromType,
            toWord = toType,
            category = TextReplacementCategory.CREATURE_TYPE
        )

        val newComponent = existingComponent?.withReplacement(replacement)
            ?: TextReplacementComponent(
                replacements = listOf(replacement)
            )

        val newState = state.updateEntity(targetId) { container ->
            container.with(newComponent)
        }

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after player chose a creature type for "reveal top card" effects.
     *
     * Reveals the top card. If it's a creature of the chosen type, put it into hand.
     * Otherwise, put it into graveyard.
     */
    fun resumeChooseCreatureTypeRevealTop(
        state: GameState,
        continuation: ChooseCreatureTypeRevealTopContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            return checkForMore(state, emptyList())
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
            typeLine.hasSubtype(Subtype(chosenType))

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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for "reveal until creature type" effects.
     *
     * Reveals cards from the top of the library until a creature of the chosen type is found.
     * Puts that creature onto the battlefield and shuffles the rest into the library.
     */
    fun resumeRevealUntilCreatureType(
        state: GameState,
        continuation: RevealUntilCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            return checkForMore(state, emptyList())
        }

        // Reveal cards from the top until we find a creature of the chosen type
        val revealedCards = mutableListOf<EntityId>()
        var matchedCard: EntityId? = null

        for (cardId in library) {
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            revealedCards.add(cardId)

            val typeLine = cardComponent?.typeLine
            if (typeLine != null &&
                typeLine.isCreature &&
                typeLine.hasSubtype(Subtype(chosenType))
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for a "becomes the creature type
     * of your choice" effect. Creates a floating effect that replaces all creature
     * subtypes with the chosen type.
     */
    fun resumeBecomeCreatureType(
        state: GameState,
        continuation: BecomeCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still be on the battlefield
        if (targetId !in state.getBattlefield()) {
            return checkForMore(state, emptyList())
        }

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
        val targetName = targetCard?.name ?: "creature"

        // Create a floating effect that sets creature subtypes
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                sublayer = null,
                modification = SerializableModification.SetCreatureSubtypes(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for stat modification.
     *
     * Creates a floating effect that modifies P/T for all creatures of the chosen type.
     */
    fun resumeChooseCreatureTypeModifyStats(
        state: GameState,
        continuation: ChooseCreatureTypeModifyStatsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            return checkForMore(state, emptyList())
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for Peer Pressure-style gain control.
     *
     * If the controller has strictly more creatures of the chosen type than each other
     * player, gains control of all creatures of that type.
     */
    fun resumeChooseCreatureTypeGainControl(
        state: GameState,
        continuation: ChooseCreatureTypeGainControlContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val result = com.wingedsheep.engine.handlers.effects.permanent.ChooseCreatureTypeGainControlExecutor.applyChooseCreatureTypeGainControl(
            state = state,
            chosenType = chosenType,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            duration = continuation.duration
        )

        return if (result.isSuccess) {
            checkForMore(result.state, result.events)
        } else {
            result
        }
    }

    /**
     * Resume after player chose a creature type for global type change.
     *
     * Sets all creatures on the battlefield to the chosen type via a floating effect.
     */
    fun resumeBecomeChosenTypeAllCreatures(
        state: GameState,
        continuation: BecomeChosenTypeAllCreaturesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            return checkForMore(state, emptyList())
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                sublayer = null,
                modification = SerializableModification.SetCreatureSubtypes(
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

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for "must attack this turn" effect.
     *
     * Marks all creatures of the chosen type on the battlefield with MustAttackThisTurnComponent.
     */
    fun resumeChooseCreatureTypeMustAttack(
        state: GameState,
        continuation: ChooseCreatureTypeMustAttackContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            if (!cardComponent.typeLine.hasSubtype(Subtype(chosenType))) continue

            // Add MustAttackThisTurnComponent
            newState = newState.updateEntity(entityId) { it.with(
                com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
            ) }
        }

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for "untap all creatures of that type" effect.
     *
     * Untaps all creatures of the chosen type on the battlefield.
     */
    fun resumeChooseCreatureTypeUntap(
        state: GameState,
        continuation: ChooseCreatureTypeUntapContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
            if (!cardComponent.typeLine.hasSubtype(Subtype(chosenType))) continue

            // Skip already untapped creatures
            if (!container.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()) continue

            // Untap the creature
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()
            }
            events.add(UntappedEvent(entityId, cardComponent.name))
        }

        return checkForMore(newState, events)
    }

    /**
     * Resume after a player chose a creature type for Harsh Mercy.
     *
     * Records the chosen type, asks the next player if any remain,
     * or destroys all creatures not of any chosen type.
     */
    fun resumeHarshMercy(
        state: GameState,
        continuation: HarshMercyContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
        val chosenSubtypes = updatedChosenTypes.map { Subtype(it) }.toSet()

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
            val result = EffectExecutorUtils.destroyPermanent(
                newState, entityId, canRegenerate = false
            )
            newState = result.newState
            events.addAll(result.events)
        }

        return checkForMore(newState, events)
    }

    /**
     * Resume after a player chooses a creature type for Patriarch's Bidding.
     * If more players remain, ask the next one. Otherwise, return all creature cards
     * matching any chosen type from all graveyards to the battlefield.
     */
    fun resumePatriarchsBidding(
        state: GameState,
        continuation: PatriarchsBiddingContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
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
        val chosenSubtypes = updatedChosenTypes.map { Subtype(it) }.toSet()

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Process each player's graveyard
        for (playerId in state.turnOrder) {
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            val graveyard = newState.getZone(graveyardZone).toList() // snapshot to avoid concurrent modification

            for (entityId in graveyard) {
                val container = newState.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                // Only creature cards
                if (!cardComponent.typeLine.isCreature) continue

                // Check if creature has any of the chosen subtypes
                val hasChosenType = cardComponent.typeLine.subtypes.any { it in chosenSubtypes }
                if (!hasChosenType) continue

                // Move from graveyard to battlefield
                val result = EffectExecutorUtils.moveCardToZone(
                    newState, entityId, Zone.BATTLEFIELD
                )
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return checkForMore(newState, events)
    }
}
