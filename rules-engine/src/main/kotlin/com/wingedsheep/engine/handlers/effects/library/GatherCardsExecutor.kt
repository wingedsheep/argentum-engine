package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GatherCardsEffect
import com.wingedsheep.sdk.scripting.Player
import kotlin.reflect.KClass

/**
 * Executor for GatherCardsEffect.
 *
 * Gathers cards from a source and stores them in a named collection
 * via [ExecutionResult.updatedCollections]. The cards are NOT removed
 * from their current zone — they are only referenced for subsequent
 * pipeline steps (SelectFromCollection, MoveCollection).
 */
class GatherCardsExecutor : EffectExecutor<GatherCardsEffect> {

    override val effectType: KClass<GatherCardsEffect> = GatherCardsEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: GatherCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val cards = when (val source = effect.source) {
            is CardSource.TopOfLibrary -> {
                val playerId = resolvePlayer(source.player, context, state)
                    ?: return ExecutionResult.error(state, "Could not resolve player for GatherCards")
                val count = amountEvaluator.evaluate(state, source.count, context)
                val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
                state.getZone(libraryZone).take(count)
            }

            is CardSource.FromZone -> {
                val playerId = resolvePlayer(source.player, context, state)
                    ?: return ExecutionResult.error(state, "Could not resolve player for GatherCards")
                val zone = ZoneKey(playerId, source.zone)
                val allCards = state.getZone(zone)
                if (source.filter != GameObjectFilter.Any) {
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { cardId ->
                        predicateEvaluator.matches(state, cardId, source.filter, predicateContext)
                    }
                } else {
                    allCards
                }
            }

            is CardSource.FromVariable -> {
                context.storedCollections[source.variableName] ?: emptyList()
            }
        }

        if (cards.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // TODO: If effect.revealed, emit CardsRevealedEvent

        return ExecutionResult.success(state).copy(
            updatedCollections = mapOf(effect.storeAs to cards)
        )
    }

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): com.wingedsheep.sdk.model.EntityId? {
        return when (player) {
            is Player.You -> context.controllerId
            is Player.Opponent -> context.opponentId
            is Player.TargetOpponent -> context.opponentId
            is Player.TargetPlayer -> context.opponentId
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { EffectExecutorUtils.run { it.toEntityId() } }
            else -> context.controllerId
        }
    }
}
