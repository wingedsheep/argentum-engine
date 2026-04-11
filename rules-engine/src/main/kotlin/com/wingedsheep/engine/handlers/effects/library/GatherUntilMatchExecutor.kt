package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * Executor for [GatherUntilMatchEffect].
 *
 * Walks a player's library top-down. For each card, evaluates the filter. On the first
 * match, stops and stores both the match and all revealed cards as named collections.
 *
 * Does NOT emit a reveal event — pair with [RevealCollectionExecutor] for that.
 *
 * Edge cases:
 * - Empty library: both collections are empty
 * - No match found: storeMatch is empty, storeRevealed contains the entire library
 */
class GatherUntilMatchExecutor : EffectExecutor<GatherUntilMatchEffect> {

    override val effectType: KClass<GatherUntilMatchEffect> = GatherUntilMatchEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: GatherUntilMatchEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayer(effect.player, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve player for GatherUntilMatch")

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        if (library.isEmpty()) {
            return ExecutionResult.success(state).copy(
                updatedCollections = mapOf(
                    effect.storeMatch to emptyList(),
                    effect.storeRevealed to emptyList()
                )
            )
        }

        val predicateContext = PredicateContext.fromEffectContext(context)
        val allRevealed = mutableListOf<EntityId>()
        var matchCard: EntityId? = null

        for (cardId in library) {
            allRevealed.add(cardId)

            if (predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)) {
                matchCard = cardId
                break
            }
        }

        val matchList = if (matchCard != null) listOf(matchCard) else emptyList()

        return ExecutionResult.success(state).copy(
            updatedCollections = mapOf(
                effect.storeMatch to matchList,
                effect.storeRevealed to allRevealed.toList()
            )
        )
    }

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): EntityId? {
        return when (player) {
            is Player.You -> context.controllerId
            is Player.Opponent -> context.opponentId
            is Player.TargetOpponent -> context.opponentId
            is Player.TargetPlayer -> context.opponentId
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            }
            is Player.TriggeringPlayer -> context.triggeringEntityId
            else -> context.controllerId
        }
    }
}
