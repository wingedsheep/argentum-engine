package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.TokenSubstitutionChoiceContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.effects.token.TokenCreationReplacementHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.core.Outcome

/**
 * Handles token-related continuation resumptions:
 * - TokenCreationReplacementContinuation (Mirrormind Crown yes/no)
 * - TokenSubstitutionChoiceContinuation (Jinnie Fay's Cats-or-Dogs pick)
 */
class TokenContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TokenCreationReplacementContinuation::class, ::resumeTokenCreationReplacement),
        resumer(TokenSubstitutionChoiceContinuation::class, ::resumeTokenSubstitutionChoice)
    )

    private fun resumeTokenSubstitutionChoice(
        state: GameState,
        continuation: TokenSubstitutionChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected an option choice for a token substitution")
        }
        val answer = if (response.optionIndex in 0 until continuation.substituteCount) response.optionIndex.toString()
        else TokenCreationReplacementHelper.SUBSTITUTION_DECLINED
        val context = continuation.effectContext.let {
            it.copy(pipeline = it.pipeline.copy(chosenValues = it.pipeline.chosenValues + (continuation.choiceKey to answer)))
        }
        val effectResult = services.effectExecutorRegistry.execute(state, continuation.originalEffect, context)
        if (effectResult.outcome is Outcome.Paused) return effectResult.toExecutionResult()
        // Publish the created tokens so a follow-up step ("they gain haste") still sees them.
        return checkForMore(exposeCollectionsToNextFrame(effectResult.state, effectResult.updatedCollections), effectResult.events)
    }

    private fun resumeTokenCreationReplacement(
        state: GameState,
        continuation: TokenCreationReplacementContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for token creation replacement")
        }

        val context = continuation.effectContext

        if (response.choice) {
            // Player chose to replace: create copies of the attached permanent.
            // Pass cardRegistry so the token applies the attached permanent's printed
            // "enters with N counters" replacement effects (per Mirrormind Crown rulings).
            val created = TokenCreationReplacementHelper.createAttachedPermanentCopies(
                state,
                continuation.attachedPermanentId,
                context.controllerId,
                continuation.tokenCount,
                cardRegistry = services.cardRegistry,
                staticAbilityHandler = StaticAbilityHandler(services.cardRegistry),
                predicateEvaluator = services.predicateEvaluator
            )
            val result = com.wingedsheep.engine.handlers.effects.token.TokenEntryGroups.stamp(created,
                com.wingedsheep.engine.handlers.effects.token.TokenEntryGroups.group(context))
            if (result.outcome is Outcome.Paused) return result.toExecutionResult()
            return checkForMore(result.state, result.events)
        } else {
            // Player declined: execute original token creation effect
            // The source is already marked as "offered this turn" so the replacement
            // won't fire again when we re-execute the original effect.
            val effectResult = services.effectExecutorRegistry.execute(
                state,
                continuation.originalEffect,
                context
            )
            if (effectResult.outcome is Outcome.Paused) return effectResult.toExecutionResult()
            return checkForMore(effectResult.state, effectResult.events)
        }
    }
}
