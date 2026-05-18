package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
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
    ): EffectResult {
        val cards = when (val source = effect.source) {
            is CardSource.TopOfLibrary -> {
                val count = amountEvaluator.evaluate(state, source.count, context)
                val playerIds = resolvePlayersForLibrary(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                playerIds.flatMap { playerId ->
                    state.getZone(ZoneKey(playerId, Zone.LIBRARY)).take(count)
                }
            }

            is CardSource.FromZone -> {
                val playerId = resolvePlayer(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                val zone = ZoneKey(playerId, source.zone)
                val allCards = state.getZone(zone)
                if (source.filter != GameObjectFilter.Any) {
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { cardId ->
                        predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
                    }
                } else {
                    allCards
                }
            }

            is CardSource.FromMultipleZones -> {
                val playerId = resolvePlayer(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                val allCards = source.zones.flatMap { zone ->
                    state.getZone(ZoneKey(playerId, zone))
                }
                if (source.filter != GameObjectFilter.Any) {
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { cardId ->
                        predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
                    }
                } else {
                    allCards
                }
            }

            is CardSource.FromVariable -> {
                context.pipeline.storedCollections[source.variableName] ?: emptyList()
            }

            is CardSource.TappedAsCost -> {
                context.tappedPermanents
            }

            is CardSource.ControlledPermanents -> {
                val playerId = resolvePlayer(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards ControlledPermanents")
                val projected = state.projectedState
                val controlled = projected.getBattlefieldControlledBy(playerId)
                if (source.filter != GameObjectFilter.Any) {
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    controlled.filter { cardId ->
                        predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
                    }
                } else {
                    controlled
                }
            }

            is CardSource.BattlefieldMatching -> {
                val resolvedPlayerId = if (source.player != Player.Each) {
                    resolvePlayer(source.player, context, state)
                        ?: return EffectResult.error(state, "Could not resolve player for GatherCards BattlefieldMatching")
                } else null
                val baseFilter = if (resolvedPlayerId != null) source.filter.youControl() else source.filter
                val excludeSelfId = if (source.excludeSelf) context.sourceId else null
                val predicateContext = PredicateContext.fromEffectContext(context).let {
                    if (resolvedPlayerId != null) it.copy(controllerId = resolvedPlayerId) else it
                }
                val afterExclusion = BattlefieldFilterUtils.findMatchingOnBattlefield(
                    state, baseFilter, predicateContext, excludeSelfId
                )
                if (source.includeAttachments) {
                    val withAttachments = afterExclusion.toMutableList()
                    for (entityId in afterExclusion) {
                        val attachments = state.getEntity(entityId)?.get<AttachmentsComponent>()
                        if (attachments != null) {
                            for (attachedId in attachments.attachedIds) {
                                if (attachedId !in withAttachments) {
                                    withAttachments.add(attachedId)
                                }
                            }
                        }
                    }
                    withAttachments
                } else {
                    afterExclusion
                }
            }

            is CardSource.ChosenTargets -> {
                context.targets.mapNotNull { chosen ->
                    when (chosen) {
                        is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> chosen.entityId
                        is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> chosen.cardId
                        is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> chosen.spellEntityId
                        is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> null
                    }
                }
            }

            is CardSource.FromLinkedExile -> {
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for FromLinkedExile")
                val sourceContainer = state.getEntity(sourceId)
                    ?: return EffectResult.error(state, "Source entity not found for FromLinkedExile")
                val linked = sourceContainer.get<LinkedExileComponent>()
                    ?: return EffectResult.success(state).copy(
                        updatedCollections = mapOf(effect.storeAs to emptyList())
                    )
                // Filter to only entities currently in exile
                val inExile = linked.exiledIds.filter { entityId ->
                    val ownerId = state.getEntity(entityId)?.get<OwnerComponent>()?.playerId
                        ?: context.controllerId
                    entityId in state.getZone(ZoneKey(ownerId, Zone.EXILE))
                }
                // Apply count limit if specified (take first N from the ordered pile)
                val count = source.count
                if (count != null) inExile.take(count) else inExile
            }
        }

        if (cards.isEmpty()) {
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to emptyList())
            )
        }

        val events = if (effect.revealed) {
            val cardNames = cards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = cards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val sourceName = context.sourceId?.let { sourceId ->
                state.getEntity(sourceId)?.get<CardComponent>()?.name
            }
            listOf(
                CardsRevealedEvent(
                    revealingPlayerId = context.controllerId,
                    cardIds = cards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName
                )
            )
        } else {
            emptyList()
        }

        // Persist reveals for cards that came from a library source.
        // - Public reveal (`revealed = true`) → revealed to every player.
        // - Private look (Scry / Surveil / look-at-top-N) → revealed to the looking player only.
        // HAND is intentionally excluded: the owner already sees their own hand, and the caster
        // should never automatically see another player's hand from a gather. Cards that genuinely
        // reveal/look at an opponent's hand use [RevealHandEffect] or [LookAtTargetHandEffect]
        // as an explicit prior step.
        val revealAudience: Set<EntityId> = when {
            effect.revealed -> state.turnOrder.toSet()
            isLibrarySource(effect.source) -> setOf(context.controllerId)
            else -> emptySet()
        }
        val newState = if (revealAudience.isNotEmpty()) {
            LibraryRevealUtils.markRevealed(state, cards, revealAudience)
        } else {
            state
        }

        return EffectResult.success(newState, events).copy(
            updatedCollections = mapOf(effect.storeAs to cards)
        )
    }

    private fun isLibrarySource(source: CardSource): Boolean = when (source) {
        is CardSource.TopOfLibrary -> true
        is CardSource.FromZone -> source.zone == Zone.LIBRARY
        is CardSource.FromMultipleZones -> source.zones.any { it == Zone.LIBRARY }
        else -> false
    }

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): com.wingedsheep.sdk.model.EntityId? {
        return when (player) {
            is Player.You -> context.controllerId
            is Player.Opponent -> context.opponentId
            is Player.TargetOpponent -> context.opponentId
            is Player.TargetPlayer -> context.targets.firstOrNull()?.let { TargetResolutionUtils.run { it.toEntityId() } }
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { TargetResolutionUtils.run { it.toEntityId() } }
            is Player.TriggeringPlayer -> context.triggeringEntityId
            is Player.OwnerOf -> context.targets.firstOrNull()?.let {
                val eid = TargetResolutionUtils.run { it.toEntityId() }
                state.getEntity(eid)?.get<CardComponent>()?.ownerId
            }
            is Player.ControllerOf -> context.targets.firstOrNull()?.let {
                val eid = TargetResolutionUtils.run { it.toEntityId() }
                state.getEntity(eid)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                    ?: state.getEntity(eid)?.get<CardComponent>()?.ownerId
            }
            else -> context.controllerId
        }
    }

    /**
     * Resolve a [Player] reference to the list of player ids whose libraries should be
     * gathered from. Multi-player references like [Player.Each] / [Player.EachOpponent]
     * fan out (turn-order; opponents in turn-order minus controller); single-player
     * references collapse to a one-element list.
     */
    private fun resolvePlayersForLibrary(
        player: Player,
        context: EffectContext,
        state: GameState
    ): List<com.wingedsheep.sdk.model.EntityId>? = when (player) {
        is Player.Each, is Player.ActivePlayerFirst -> state.turnOrder
        is Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
        else -> resolvePlayer(player, context, state)?.let { listOf(it) }
    }
}
