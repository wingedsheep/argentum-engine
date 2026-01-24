package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID

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
        legalTargets: Map<Int, List<EntityId>>
    ): ExecutionResult {
        val decision = ChooseTargetsDecision(
            id = generateDecisionId(),
            playerId = playerId,
            prompt = "Choose targets for $sourceName",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            targetRequirements = requirements,
            legalTargets = legalTargets
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "CHOOSE_TARGETS",
                    prompt = decision.prompt
                )
            )
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
        phase: DecisionPhase = DecisionPhase.RESOLUTION
    ): ExecutionResult {
        val decision = SelectCardsDecision(
            id = generateDecisionId(),
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
            ordered = ordered
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
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
        phase: DecisionPhase = DecisionPhase.RESOLUTION
    ): ExecutionResult {
        val decision = YesNoDecision(
            id = generateDecisionId(),
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = phase
            ),
            yesText = yesText,
            noText = noText
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
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
        maxModes: Int = 1
    ): ExecutionResult {
        val decision = ChooseModeDecision(
            id = generateDecisionId(),
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
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "CHOOSE_MODE",
                    prompt = decision.prompt
                )
            )
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
        phase: DecisionPhase = DecisionPhase.RESOLUTION
    ): ExecutionResult {
        val decision = ChooseColorDecision(
            id = generateDecisionId(),
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = phase
            )
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "CHOOSE_COLOR",
                    prompt = decision.prompt
                )
            )
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
        minPerTarget: Int = 0
    ): ExecutionResult {
        val decision = DistributeDecision(
            id = generateDecisionId(),
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
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "DISTRIBUTE",
                    prompt = decision.prompt
                )
            )
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
        phase: DecisionPhase = DecisionPhase.RESOLUTION
    ): ExecutionResult {
        val decision = OrderObjectsDecision(
            id = generateDecisionId(),
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = phase
            ),
            objects = objects
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "ORDER_OBJECTS",
                    prompt = decision.prompt
                )
            )
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
        pileLabels: List<String> = emptyList()
    ): ExecutionResult {
        val decision = SplitPilesDecision(
            id = generateDecisionId(),
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
        )

        val newState = state.withPendingDecision(decision)
        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "SPLIT_PILES",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun generateDecisionId(): String = UUID.randomUUID().toString()
}
