package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import kotlin.reflect.KClass

/**
 * Executor for GrantMayPlayFromExileEffect.
 *
 * Marks all cards in a named collection with MayPlayFromExileComponent,
 * granting the controller permission to play them from exile until end of turn.
 */
class GrantMayPlayFromExileExecutor : EffectExecutor<GrantMayPlayFromExileEffect> {

    override val effectType: KClass<GrantMayPlayFromExileEffect> = GrantMayPlayFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantMayPlayFromExileEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        val isPermanent = effect.expiry is MayPlayExpiry.Permanent
        val expiresAfterTurn = expiresAfterTurnFor(state, controllerId, effect.expiry)

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(
                    MayPlayFromExileComponent(
                        controllerId = controllerId,
                        permanent = isPermanent,
                        expiresAfterTurn = expiresAfterTurn,
                        withAnyManaType = effect.withAnyManaType
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }

    /**
     * Translate a [MayPlayExpiry] into the turn whose cleanup will remove the permission.
     * Returns `null` for [MayPlayExpiry.EndOfTurn] (default handling: cleared this cleanup)
     * and for [MayPlayExpiry.Permanent] (component is flagged permanent and skipped).
     */
    private fun expiresAfterTurnFor(
        state: GameState,
        controllerId: EntityId,
        expiry: MayPlayExpiry
    ): Int? = when (expiry) {
        MayPlayExpiry.EndOfTurn, MayPlayExpiry.Permanent -> null
        is MayPlayExpiry.UntilControllerStep -> resolveStepTurn(state, controllerId, expiry)
    }

    /**
     * Resolve which turn's cleanup will mark "the controller's next [step]" given the
     * current step and active player. The cleanup-driven removal is coarse — it runs once
     * per turn at cleanup — so we map any step in the turn to that turn's cleanup.
     */
    private fun resolveStepTurn(
        state: GameState,
        controllerId: EntityId,
        expiry: MayPlayExpiry.UntilControllerStep
    ): Int {
        val turnOrder = state.turnOrder
        val playerIndex = turnOrder.indexOf(controllerId)
        val activeIndex = turnOrder.indexOf(state.activePlayerId)
        val playerCount = turnOrder.size

        val onControllerTurn = playerIndex == activeIndex
        val targetStep = expiry.step
        val targetReachedThisTurn = state.step.ordinal >= targetStep.ordinal
        val thisTurnStillCounts = onControllerTurn && expiry.includeCurrentTurn && !targetReachedThisTurn

        return if (thisTurnStillCounts) {
            state.turnNumber
        } else {
            val turnsUntilNext = if (onControllerTurn) {
                playerCount
            } else {
                (playerIndex - activeIndex + playerCount) % playerCount
            }
            state.turnNumber + turnsUntilNext
        }
    }
}

/**
 * Executor for GrantPlayWithoutPayingCostEffect.
 *
 * Marks all cards in a named collection with PlayWithoutPayingCostComponent,
 * allowing the controller to play them without paying mana cost until end of turn.
 */
class GrantPlayWithoutPayingCostExecutor : EffectExecutor<GrantPlayWithoutPayingCostEffect> {

    override val effectType: KClass<GrantPlayWithoutPayingCostEffect> = GrantPlayWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithoutPayingCostEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            }
        }

        return EffectResult.success(newState)
    }
}
