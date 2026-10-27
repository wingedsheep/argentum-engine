package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles the creation and resolution of player decisions.
 *
 * When the engine needs player input (choosing targets, selecting cards, etc.),
 * it creates a PendingDecision and pauses. When the player responds, this handler
 * validates and processes the response.
 */
class DecisionHandler {

    /**
     * Creates a target selection decision.
     *
     * @param state Current game state
     * @param playerId Player who must choose
     * @param sourceId The spell/ability requesting targets
     * @param sourceName Name of the source for display
     * @param requirements List of target requirements
     * @param legalTargets Map of requirement index to valid target IDs
     */
    fun createTargetDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        requirements: List<TargetRequirementInfo>,
        legalTargets: Map<Int, List<EntityId>>,
        effectHint: String? = null,
        candidateZones: Set<com.wingedsheep.sdk.core.Zone>? = null,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> ChooseTargetsDecision(
                id = decisionId,
                playerId = playerId,
                prompt = "Choose targets for $sourceName",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.CASTING,
                    effectHint = effectHint,
                    candidateZones = candidateZones
                ),
                targetRequirements = requirements,
                legalTargets = legalTargets
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a card selection decision (for discard, sacrifice, search, etc.).
     */
    fun createCardSelectionDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        prompt: String,
        options: List<EntityId>,
        minSelections: Int,
        maxSelections: Int,
        ordered: Boolean = false,
        phase: DecisionPhase = DecisionPhase.RESOLUTION,
        useTargetingUI: Boolean = false,
        /**
         * Floor on the summed mana value of the selection — collect evidence N (CR 701.59a).
         * See [SelectCardsDecision.minTotalManaValue].
         */
        minTotalManaValue: Int? = null,
        selectionLimits: Map<EntityId, Int>? = null,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> SelectCardsDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = phase
                ),
                options = options,
                minSelections = minSelections,
                maxSelections = maxSelections,
                ordered = ordered,
                useTargetingUI = useTargetingUI,
                minTotalManaValue = minTotalManaValue,
                selectionLimits = selectionLimits
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a yes/no decision (for may abilities).
     */
    fun createYesNoDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        prompt: String,
        yesText: String = "Yes",
        noText: String = "No",
        phase: DecisionPhase = DecisionPhase.RESOLUTION,
        abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> YesNoDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = phase,
                    abilityIdentity = abilityIdentity
                ),
                yesText = yesText,
                noText = noText
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a mode selection decision (for modal spells).
     */
    fun createModeDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        modes: List<ModeOption>,
        minModes: Int = 1,
        maxModes: Int = 1,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> ChooseModeDecision(
                id = decisionId,
                playerId = playerId,
                prompt = "Choose ${if (minModes == maxModes) minModes else "$minModes-$maxModes"} mode(s) for $sourceName",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.CASTING
                ),
                modes = modes,
                minModes = minModes,
                maxModes = maxModes
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a color choice decision.
     */
    fun createColorDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        prompt: String,
        phase: DecisionPhase = DecisionPhase.RESOLUTION,
        availableColors: Set<Color> = Color.entries.toSet(),
        answer: AnswerContinuation,
        maxColors: Int = 1,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> ChooseColorDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = phase
                ),
                availableColors = availableColors,
                maxColors = maxColors.coerceAtMost(availableColors.size).coerceAtLeast(1)
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a distribution decision (for dividing damage, etc.).
     */
    fun createDistributeDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        prompt: String,
        totalAmount: Int,
        targets: List<EntityId>,
        minPerTarget: Int = 0,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> DistributeDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                totalAmount = totalAmount,
                targets = targets,
                minPerTarget = minPerTarget
            ) },
            answer = answer,
        )
    }

    /**
     * Creates an ordering decision (for scry, damage assignment, etc.).
     */
    fun createOrderDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        prompt: String,
        objects: List<EntityId>,
        phase: DecisionPhase = DecisionPhase.RESOLUTION,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> OrderObjectsDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = phase
                ),
                objects = objects
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a pile split decision (for Fact or Fiction, etc.).
     */
    fun createPileSplitDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        cards: List<EntityId>,
        numberOfPiles: Int = 2,
        pileLabels: List<String> = emptyList(),
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> SplitPilesDecision(
                id = decisionId,
                playerId = playerId,
                prompt = "Separate cards into $numberOfPiles piles",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                cards = cards,
                numberOfPiles = numberOfPiles,
                pileLabels = pileLabels
            ) },
            answer = answer,
        )
    }

    /**
     * Creates a number choice decision.
     */
    fun createNumberDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        prompt: String,
        minValue: Int,
        maxValue: Int,
        phase: DecisionPhase = DecisionPhase.RESOLUTION,
        answer: AnswerContinuation,
    ): ExecutionResult {
        return state.suspendForDecision(
            question = { decisionId -> ChooseNumberDecision(
                id = decisionId,
                playerId = playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = phase
                ),
                minValue = minValue,
                maxValue = maxValue
            ) },
            answer = answer,
        )
    }

}
