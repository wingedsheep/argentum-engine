package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.effects.token.TokenCreationReplacementHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState

/**
 * Handles token-related continuation resumptions:
 * - TokenCreationReplacementContinuation (Mirrormind Crown yes/no)
 */
class TokenContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TokenCreationReplacementContinuation::class, ::resumeTokenCreationReplacement)
    )

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
            // Player chose to replace: create copies of equipped creature.
            // Pass cardRegistry so the token applies the equipped creature's printed
            // "enters with N counters" replacement effects (per Mirrormind Crown rulings).
            val result = TokenCreationReplacementHelper.createEquippedCreatureCopies(
                state,
                continuation.equippedCreatureId,
                context.controllerId,
                continuation.tokenCount,
                cardRegistry = services.cardRegistry,
                staticAbilityHandler = StaticAbilityHandler(services.cardRegistry)
            )
            if (result.isPaused) return result.toExecutionResult()
            return checkForMore(result.state, result.events)
        } else {
            // Player declined: execute original token creation effect
            // The equipment is already marked as "offered this turn" so the replacement
            // won't fire again when we re-execute the original effect.
            val effectResult = services.effectExecutorRegistry.execute(
                state,
                continuation.originalEffect,
                context
            )
            if (effectResult.isPaused) return effectResult.toExecutionResult()
            return checkForMore(effectResult.state, effectResult.events)
        }
    }
}
