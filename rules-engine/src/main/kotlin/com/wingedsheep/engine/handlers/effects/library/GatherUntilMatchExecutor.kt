package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
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
 * Walks a player's library top-down, collecting cards until [GatherUntilMatchEffect.count]
 * matching cards have been revealed (or the library runs out). Stores the matches and
 * all revealed cards as named collections.
 *
 * Does NOT emit a reveal event — pair with [RevealCollectionExecutor] for that.
 *
 * Edge cases:
 * - Empty library: both collections are empty
 * - Count evaluates to ≤ 0: both collections are empty (no cards walked)
 * - Fewer matches than count: storeMatch has what was found; storeRevealed is the whole library
 */
class GatherUntilMatchExecutor : EffectExecutor<GatherUntilMatchEffect> {

    override val effectType: KClass<GatherUntilMatchEffect> = GatherUntilMatchEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: GatherUntilMatchEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = resolvePlayer(effect.player, context, state)
            ?: return EffectResult.error(state, "Could not resolve player for GatherUntilMatch")

        val targetCount = amountEvaluator.evaluate(state, effect.count, context)
        if (targetCount <= 0) {
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(
                    effect.storeMatch to emptyList(),
                    effect.storeRevealed to emptyList()
                )
            )
        }

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        if (library.isEmpty()) {
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(
                    effect.storeMatch to emptyList(),
                    effect.storeRevealed to emptyList()
                )
            )
        }

        val predicateContext = PredicateContext.fromEffectContext(context)
        val allRevealed = mutableListOf<EntityId>()
        val matches = mutableListOf<EntityId>()

        for (cardId in library) {
            allRevealed.add(cardId)

            if (predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)) {
                matches.add(cardId)
                if (matches.size >= targetCount) break
            }
        }

        return EffectResult.success(state).copy(
            updatedCollections = mapOf(
                effect.storeMatch to matches.toList(),
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
