package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ModalEffect.
 * Handles "Choose one —" / "Choose two —" modal spells by presenting a mode selection.
 *
 * If the mode was already chosen at cast time (stored in SpellOnStackComponent.chosenModes),
 * the executor skips the mode selection decision and directly executes the pre-chosen mode.
 *
 * Flow (mode NOT pre-chosen):
 * 1. Present mode options to the player (ChooseOptionDecision)
 * 2. Push ModalContinuation with mode data
 * 3. ContinuationHandler handles the response: executes chosen mode's effect,
 *    pausing for target selection if needed.
 *
 * Flow (mode pre-chosen at cast time):
 * 1. Read chosenModes from SpellOnStackComponent
 * 2. Directly execute the chosen mode's effect with pre-selected targets
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class ModalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ModalEffect> {

    override val effectType: KClass<ModalEffect> = ModalEffect::class

    override fun execute(
        state: GameState,
        effect: ModalEffect,
        context: EffectContext
    ): EffectResult {
        // Check if mode was already chosen at cast time
        val spellOnStack = context.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        if (spellOnStack != null && spellOnStack.chosenModes.isNotEmpty()) {
            val modeIndex = spellOnStack.chosenModes.first()
            val chosenMode = effect.modes.getOrNull(modeIndex)
                ?: return EffectResult.error(state, "Invalid pre-chosen mode index: $modeIndex")

            // Execute the pre-chosen mode directly (targets were already selected at cast time)
            return effectExecutor(state, chosenMode.effect, context)
        }

        // Mode not pre-chosen — present mode selection decision (legacy flow for
        // triggered/activated modal abilities, and "Choose N" modal spells like Commands).
        val playerId = context.controllerId

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Build mode descriptions for the decision
        val modeDescriptions = effect.modes.map { it.description }
        val availableIndices = effect.modes.indices.toList()

        val basePrompt = "Choose a mode for ${sourceName ?: "modal spell"}"
        val prompt = if (effect.chooseCount > 1) "$basePrompt (1 of ${effect.chooseCount})" else basePrompt

        // Create option decision for mode selection
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = modeDescriptions
        )

        // Create continuation to resume after player's choice
        val continuation = ModalContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            modes = effect.modes,
            xValue = context.xValue,
            opponentId = context.opponentId,
            triggeringEntityId = context.triggeringEntityId,
            chooseCount = effect.chooseCount,
            selectedModeIndices = emptyList(),
            availableIndices = availableIndices
        )

        // Push continuation and return paused state
        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
