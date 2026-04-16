package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ExileLibraryUntilManaValueEffect
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * Executor for [ExileLibraryUntilManaValueEffect].
 *
 * For each matching player, walks that player's library top-down and exiles
 * cards until the cumulative mana value of cards exiled this way reaches the
 * threshold. {X} contributes 0 (rule 107.3b). All exiled card IDs are
 * accumulated into [ExileLibraryUntilManaValueEffect.storeAs] on the outer
 * pipeline so downstream grants operate under the spell's original controller.
 */
class ExileLibraryUntilManaValueExecutor : EffectExecutor<ExileLibraryUntilManaValueEffect> {

    override val effectType: KClass<ExileLibraryUntilManaValueEffect> = ExileLibraryUntilManaValueEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: ExileLibraryUntilManaValueEffect,
        context: EffectContext
    ): EffectResult {
        val threshold = amountEvaluator.evaluate(state, effect.threshold, context)
        val targetPlayers = resolvePlayers(effect.players, state, context)
        if (targetPlayers.isEmpty()) {
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to emptyList())
            )
        }

        var currentState = state
        val allEvents = mutableListOf<EngineGameEvent>()
        val allExiled = mutableListOf<EntityId>()

        for (playerId in targetPlayers) {
            val exiledForPlayer = mutableListOf<EntityId>()
            var totalMv = 0

            val library = currentState.getZone(ZoneKey(playerId, Zone.LIBRARY))
            for (cardId in library) {
                val mv = currentState.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0
                exiledForPlayer.add(cardId)
                totalMv += mv
                if (totalMv >= threshold) break
            }

            if (exiledForPlayer.isEmpty()) continue

            val cardNames = exiledForPlayer.map { cardId ->
                currentState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = exiledForPlayer.map { cardId ->
                currentState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val sourceName = context.sourceId?.let {
                currentState.getEntity(it)?.get<CardComponent>()?.name
            }
            allEvents.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = exiledForPlayer.toList(),
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName
                )
            )

            for (cardId in exiledForPlayer) {
                val result = ZoneMovementUtils.moveCardToZone(currentState, cardId, Zone.EXILE)
                if (result.isSuccess) {
                    currentState = result.state
                    allEvents.addAll(result.events)
                    allExiled.add(cardId)
                }
            }
        }

        return EffectResult.success(currentState, allEvents).copy(
            updatedCollections = mapOf(effect.storeAs to allExiled.toList())
        )
    }

    private fun resolvePlayers(player: Player, state: GameState, context: EffectContext): List<EntityId> {
        return when (player) {
            Player.Each, Player.ActivePlayerFirst -> state.turnOrder
            Player.You -> listOf(context.controllerId)
            Player.Opponent, Player.EachOpponent, Player.TargetOpponent ->
                state.turnOrder.filter { it != context.controllerId }
            Player.TargetPlayer -> context.targets.firstOrNull()?.let {
                listOf(TargetResolutionUtils.run { it.toEntityId() })
            } ?: emptyList()
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let {
                listOf(TargetResolutionUtils.run { it.toEntityId() })
            } ?: emptyList()
            Player.TriggeringPlayer -> listOfNotNull(context.triggeringEntityId)
            else -> state.turnOrder.filter { it != context.controllerId }
        }
    }
}
