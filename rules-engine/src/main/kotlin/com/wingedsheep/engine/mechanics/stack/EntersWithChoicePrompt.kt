package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Presents the decision an "as this permanent enters, choose …" replacement effect asks for
 * (CR 614.12), pausing resolution with the [EntersWithChoiceSpellContinuation] that records the
 * answer.
 */
internal class EntersWithChoicePrompt(
    private val cardRegistry: CardRegistry
) {
    /**
     * Create the appropriate decision and continuation for an EntersWithChoice replacement effect.
     * Returns null if the choice cannot be presented (e.g., no creatures on battlefield for CREATURE_ON_BATTLEFIELD).
     */
    internal fun pauseForEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        syntheticRiot: Boolean = false,
        syntheticRiotRemaining: Int = 0
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            com.wingedsheep.sdk.scripting.references.Player.AnOpponent ->
                state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR ->
                promptColor(state, spellId, controllerId, ownerId, cardComponent, choice, chooserId)
            ChoiceType.CREATURE_TYPE ->
                promptCreatureType(state, spellId, controllerId, ownerId, cardComponent, choice, chooserId)
            ChoiceType.CREATURE_ON_BATTLEFIELD ->
                promptCreatureYouControl(state, spellId, controllerId, ownerId, cardComponent, choice)
            ChoiceType.MODE ->
                promptMode(
                    state, spellId, controllerId, ownerId, cardComponent, choice, chooserId,
                    syntheticRiot, syntheticRiotRemaining
                )
            ChoiceType.BASIC_LAND_TYPE ->
                promptBasicLandType(state, spellId, controllerId, ownerId, cardComponent, chooserId)
            ChoiceType.OPPONENT ->
                promptOpponent(state, spellId, controllerId, ownerId, cardComponent, chooserId)
            ChoiceType.CARD_NAME ->
                promptCardName(state, spellId, controllerId, ownerId, cardComponent, choice, chooserId)
            ChoiceType.NUMBER ->
                promptNumber(state, spellId, controllerId, ownerId, cardComponent, choice, chooserId)
        }
    }

    private fun promptColor(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        chooserId: EntityId
    ): ExecutionResult? {
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.COLOR
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseColorDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = colorChoicePrompt(choice),
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    availableColors = Color.entries.toSet() - choice.excludedColors
                )
            },
            answer = continuation
        )
    }

    private fun promptCreatureType(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        chooserId: EntityId
    ): ExecutionResult? {
        val creatureTypeOptions = choice.allowedCreatureTypes
            ?: com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.CREATURE_TYPE,
            creatureTypes = creatureTypeOptions
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = creatureTypeOptions,
                    defaultSearch = ""
                )
            },
            answer = continuation
        )
    }

    private fun promptCreatureYouControl(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice
    ): ExecutionResult? {
        val candidates = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
            .chosenPermanentCandidates(state, spellId, controllerId, choice)
        if (candidates.isEmpty()) return null // Nothing to choose — enter without choice
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD
        )
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                        .chosenPermanentPrompt(cardComponent, choice),
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = candidates,
                    minSelections = if (choice.mayChooseNothing) 0 else 1,
                    maxSelections = 1,
                    useTargetingUI = true
                )
            },
            answer = continuation
        )
    }

    private fun promptMode(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        chooserId: EntityId,
        syntheticRiot: Boolean,
        syntheticRiotRemaining: Int
    ): ExecutionResult? {
        if (choice.modeOptions.isEmpty()) {
            return null
        }
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.MODE,
            modeOptionIds = choice.modeOptions.map { it.id },
            syntheticRiot = syntheticRiot,
            syntheticRiotRemaining = syntheticRiotRemaining
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose for ${cardComponent.name}",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = choice.modeOptions.map { it.label },
                    optionMetadata = choice.modeOptions.map {
                        OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                    }
                )
            },
            answer = continuation
        )
    }

    private fun promptBasicLandType(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        chooserId: EntityId
    ): ExecutionResult? {
        val landTypeOptions = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES.toList()
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.BASIC_LAND_TYPE,
            landTypes = landTypeOptions
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a basic land type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = landTypeOptions,
                    defaultSearch = ""
                )
            },
            answer = continuation
        )
    }

    private fun promptOpponent(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        chooserId: EntityId
    ): ExecutionResult? {
        // CR 614.12a — replacement-effect choices that modify how a permanent enters
        // are made before the permanent enters. We surface the opponent prompt now so
        // the chosen opponent is durably recorded in [CastChoicesComponent]. In a 1v1
        // game this collapses to a forced choice but the prompt is still surfaced.
        val opponentIds = state.turnOrder.filter { it != chooserId }
        if (opponentIds.isEmpty()) return null
        val opponentNames = opponentIds.map { pid ->
            state.getEntity(pid)
                ?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name
                ?: "Player ${pid.value}"
        }
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.OPPONENT,
            opponentIds = opponentIds
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose an opponent",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = opponentNames
                )
            },
            answer = continuation
        )
    }

    private fun promptCardName(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        chooserId: EntityId
    ): ExecutionResult? {
        // "Choose a land card name" (Petrified Hamlet) or "choose any card name"
        // (Sorcerous Spyglass / Pithing Needle) as the permanent spell resolves. The pool
        // is land names or every registered card name per [EntersWithChoice.cardNamePool];
        // the chosen name is stored durably under [ChoiceSlot.CARD_NAME] by the resumer.
        val cardNames = cardRegistry.cardNamesIn(choice.cardNamePool).sorted()
        if (cardNames.isEmpty()) return null
        // "As this enters, look at an opponent's hand, then …": reveal the opponent's hand
        // to the controller before presenting the name choice.
        val (baseState, lookEvents) = if (choice.lookAtOpponentHand) {
            com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                .revealOpponentHandForEntersChoice(state, controllerId)
        } else state to emptyList()
        val prompt = choice.cardNamePool.prompt
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.CARD_NAME,
            cardNames = cardNames
        )
        return baseState.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = prompt,
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = cardNames
                )
            },
            answer = continuation,
            events = lookEvents
        )
    }

    private fun promptNumber(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        chooserId: EntityId
    ): ExecutionResult? {
        // "As this creature enters, choose a number between [min] and [max]" (Shapeshifter).
        // The chosen number is stored durably under [ChoiceSlot.CHOSEN_NUMBER] by the resumer.
        val continuation = EntersWithChoiceSpellContinuation(
            spellId = spellId,
            controllerId = controllerId,
            ownerId = ownerId,
            choiceType = ChoiceType.NUMBER
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseNumberDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a number between ${choice.minValue} and ${choice.maxValue}",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    minValue = choice.minValue,
                    maxValue = choice.maxValue
                )
            },
            answer = continuation
        )
    }
}

/**
 * The prompt for an [EntersWithChoice] of [ChoiceType.COLOR] — "Choose a color", or "Choose a color
 * other than red" when the choice excludes colors (the Thriving lands). The exclusion itself is
 * enforced by the decision's `availableColors`; this only keeps the prompt honest about it.
 */
internal fun colorChoicePrompt(choice: EntersWithChoice): String =
    if (choice.excludedColors.isEmpty()) {
        "Choose a color"
    } else {
        "Choose a color other than " +
            choice.excludedColors.sortedBy { it.ordinal }.joinToString(" or ") { it.displayName.lowercase() }
    }
