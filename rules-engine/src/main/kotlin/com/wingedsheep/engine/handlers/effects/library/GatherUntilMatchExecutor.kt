package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * Executor for [GatherUntilMatchEffect].
 *
 * Walks a player's library top-down (each player's, for [Player.Each] / [Player.EachOpponent] /
 * [Player.ActivePlayerFirst], accumulating into one collection), collecting cards until [GatherUntilMatchEffect.count]
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
class GatherUntilMatchExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<GatherUntilMatchEffect> {
    private val amountEvaluator = predicateEvaluator.amounts

    override val effectType: KClass<GatherUntilMatchEffect> = GatherUntilMatchEffect::class

    override fun execute(
        state: GameState,
        effect: GatherUntilMatchEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = resolvePlayers(effect.player, context, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "Could not resolve player for GatherUntilMatch")
        }

        val targetCount = amountEvaluator.evaluate(state, effect.count, context)
        if (targetCount <= 0) {
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

        // Each player's library is walked independently (count matches per library) and the
        // results accumulate into the same two collections, in player order.
        for (playerId in playerIds) {
            var found = 0
            for (cardId in state.getZone(ZoneKey(playerId, Zone.LIBRARY))) {
                allRevealed.add(cardId)
                if (predicateEvaluator.matches(state, state.projectedState, cardId, effect.filter, predicateContext)) {
                    matches.add(cardId)
                    if (++found >= targetCount) break
                }
            }
        }

        return EffectResult.success(state).copy(
            updatedCollections = mapOf(
                effect.storeMatch to matches.toList(),
                effect.storeRevealed to allRevealed.toList()
            )
        )
    }

    private fun resolvePlayers(player: Player, context: EffectContext, state: GameState): List<EntityId> =
        when (player) {
            Player.Each -> state.activePlayers
            Player.ActivePlayerFirst -> state.apnapOrder
            Player.EachOpponent -> state.getOpponents(context.controllerId)
            else -> listOf(TargetResolutionUtils.resolvePlayerRef(player, context, state) ?: context.controllerId)
        }
}
