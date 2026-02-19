package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.RevealUntilEffect
import kotlin.reflect.KClass

/**
 * Executor for RevealUntilEffect.
 *
 * Reveals cards from the top of a player's library until a card matching
 * the filter is found. Stores the matching card and all revealed cards
 * in named collections for subsequent pipeline steps.
 *
 * This is a gather-style pipeline step: cards are NOT removed from the
 * library. Subsequent MoveCollectionEffect steps handle the actual zone changes.
 *
 * When [RevealUntilEffect.matchChosenCreatureType] is true, the match condition
 * additionally requires the card to be a creature with the subtype stored in
 * [EffectContext.chosenCreatureType] (set by a preceding ChooseCreatureTypeEffect).
 *
 * Edge cases:
 * - Empty library: both collections are empty, no events
 * - No match found: storeMatch is empty, storeRevealed has all cards
 */
class RevealUntilExecutor : EffectExecutor<RevealUntilEffect> {

    override val effectType: KClass<RevealUntilEffect> = RevealUntilEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: RevealUntilEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayer(effect.source, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve player for RevealUntil")

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
        val chosenType = if (effect.matchChosenCreatureType) context.chosenCreatureType else null
        val allRevealed = mutableListOf<EntityId>()
        var matchCard: EntityId? = null

        for (cardId in library) {
            allRevealed.add(cardId)

            // Check base filter first
            if (!predicateEvaluator.matches(state, cardId, effect.matchFilter, predicateContext)) {
                continue
            }

            // If also matching chosen creature type, verify the card is a creature of that type
            if (chosenType != null) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
                val typeLine = cardComponent?.typeLine
                if (typeLine == null || !typeLine.isCreature || !typeLine.hasSubtype(Subtype(chosenType))) {
                    continue
                }
            }

            matchCard = cardId
            break
        }

        val events = if (allRevealed.isNotEmpty()) {
            val cardNames = allRevealed.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = allRevealed.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

            listOf(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = allRevealed.toList(),
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName
                )
            )
        } else {
            emptyList()
        }

        val matchList = if (matchCard != null) listOf(matchCard) else emptyList()

        return ExecutionResult.success(state, events).copy(
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
                EffectExecutorUtils.run { it.toEntityId() }
            }
            is Player.TriggeringPlayer -> context.triggeringEntityId
            else -> context.controllerId
        }
    }
}
