package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeWordInTextExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent

/**
 * Applies a text change once the player has picked both the FROM and TO words in a single
 * [ChooseReplacementDecision] (Crystal Spray, Artificial Evolution). Resolves the chosen indices
 * back to words, derives the replacement category from the continuation's [ReplacementMode], and
 * attaches the [TextReplacementComponent] to the target.
 */
class TextReplacementContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChooseReplacementContinuation::class, ::resume)
    )

    fun resume(
        state: GameState,
        continuation: ChooseReplacementContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ReplacementChosenResponse) {
            return ExecutionResult.error(state, "Expected replacement response for text change")
        }

        val fromWord = continuation.fromOptions.getOrNull(response.fromIndex)
            ?: return ExecutionResult.error(state, "Invalid from index: ${response.fromIndex}")
        val toWord = continuation.toOptions.getOrNull(response.toIndex)
            ?: return ExecutionResult.error(state, "Invalid to index: ${response.toIndex}")

        val targetId = continuation.targetId
        if (state.getEntity(targetId) == null) {
            return checkForMore(state, emptyList())
        }

        val category = when (continuation.mode) {
            ReplacementMode.CREATURE_TYPE -> TextReplacementCategory.CREATURE_TYPE
            ReplacementMode.WORD ->
                if (fromWord in ChangeWordInTextExecutor.COLOR_WORDS) TextReplacementCategory.COLOR_WORD
                else TextReplacementCategory.BASIC_LAND_TYPE
        }

        val replacement = TextReplacement(
            fromWord = fromWord,
            toWord = toWord,
            category = category,
            duration = continuation.duration
        )

        val existing = state.getEntity(targetId)?.get<TextReplacementComponent>()
        val newComponent = existing?.withReplacement(replacement)
            ?: TextReplacementComponent(replacements = listOf(replacement))

        val newState = state.updateEntity(targetId) { container ->
            container.with(newComponent)
        }

        return checkForMore(newState, listOf(TextChangedEvent(targetId, fromWord, toWord)))
    }
}
