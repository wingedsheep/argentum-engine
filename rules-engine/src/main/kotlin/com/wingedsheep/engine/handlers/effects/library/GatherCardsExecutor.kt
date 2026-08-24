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
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.stack.entityIds
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
        val gathered = when (val source = effect.source) {
            is CardSource.TopOfLibrary -> {
                val count = amountEvaluator.evaluate(state, source.count, context)
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                playerIds.flatMap { playerId ->
                    // For a mill, apply ModifyMillAmount replacement effects to the announced
                    // count per milling player (CR 701.13 — "mill that many plus four instead").
                    val effectiveCount = if (source.isMill) {
                        MillAmountModifier.apply(state, playerId, count)
                    } else {
                        count
                    }
                    state.getZone(ZoneKey(playerId, Zone.LIBRARY)).take(effectiveCount)
                }
            }

            is CardSource.FromZone -> {
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                val allCards = playerIds.flatMap { playerId ->
                    state.getZone(ZoneKey(playerId, source.zone))
                }
                val filtered = if (source.filter != GameObjectFilter.Any) {
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { cardId ->
                        predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
                    }
                } else {
                    allCards
                }
                // "return another permanent card …": drop anything sacrificed to this same
                // spell/ability (Rise of the Witch-king, LTR). The sacrificed cards retain
                // their entity IDs in the graveyard, so the snapshot IDs identify them.
                if (source.excludeSacrificedThisWay) {
                    val sacrificedIds = context.sacrificedPermanents.entityIds.toSet()
                    filtered.filter { it !in sacrificedIds }
                } else {
                    filtered
                }
            }

            is CardSource.FromMultipleZones -> {
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                val allCards = playerIds.flatMap { playerId ->
                    source.zones.flatMap { zone ->
                        state.getZone(ZoneKey(playerId, zone))
                    }
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

            // The cards exiled to pay this ability's activation cost, recorded at payment time.
            // Restricted to cards still in exile: a copy effect or another player's response can
            // move one out between activation and resolution, and "those exiled cards" can only
            // mean the ones that are still there to act on.
            is CardSource.ExiledAsCost -> {
                context.exiledAsCostCards.filter { cardId ->
                    val ownerId = state.getEntity(cardId)?.get<OwnerComponent>()?.playerId
                        ?: context.controllerId
                    cardId in state.getZone(ZoneKey(ownerId, Zone.EXILE))
                }
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
                val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(
                    state, baseFilter, predicateContext, excludeSelfId
                )
                val afterTriggering = if (source.excludeTriggering) {
                    matched.filter { it != context.triggeringEntityId }
                } else {
                    matched
                }
                // Exclude the spell/ability's chosen targets ("airbend all *other* creatures",
                // where "other" is relative to the chosen target — Avatar's Wrath).
                val afterExclusion = if (source.excludeChosenTargets) {
                    val chosenIds = context.targets.mapNotNull { chosen ->
                        when (chosen) {
                            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> chosen.entityId
                            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> chosen.cardId
                            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> chosen.spellEntityId
                            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> null
                        }
                    }.toSet()
                    afterTriggering.filter { it !in chosenIds }
                } else {
                    afterTriggering
                }
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

            is CardSource.AttachedTo -> {
                // Permanents attached to the resolved host that match the filter (projected).
                // Intersect the host's live attachments with the projected filter matches so
                // type/control-changing effects are respected (e.g. "Equipment attached to that
                // creature"). Empty when the host left play or has no matching attachments.
                val hostId = context.resolveTarget(source.host)
                val attachedIds = hostId
                    ?.let { state.getEntity(it)?.get<AttachmentsComponent>()?.attachedIds }
                    ?: emptyList()
                if (attachedIds.isEmpty()) {
                    emptyList()
                } else {
                    val matching = BattlefieldFilterUtils
                        .findMatchingOnBattlefield(state, source.filter, context)
                        .toSet()
                    attachedIds.filter { it in matching }
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

            is CardSource.CraftedMaterials -> {
                // The cards exiled to Craft this permanent (CraftedFromExiledComponent), filtered to
                // those still in exile — the gather-pipeline twin of DonorCards.CRAFT_MATERIALS.
                // Backs The Grim Captain's "put an exiled creature card used to craft it" clause.
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for CraftedMaterials")
                val sourceContainer = state.getEntity(sourceId)
                    ?: return EffectResult.error(state, "Source entity not found for CraftedMaterials")
                val crafted = sourceContainer.get<CraftedFromExiledComponent>()
                    ?: return EffectResult.success(state).copy(
                        updatedCollections = mapOf(effect.storeAs to emptyList())
                    )
                crafted.exiledIds.filter { entityId ->
                    val ownerId = state.getEntity(entityId)?.get<OwnerComponent>()?.playerId
                        ?: context.controllerId
                    entityId in state.getZone(ZoneKey(ownerId, Zone.EXILE))
                }
            }

            is CardSource.Self -> {
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for CardSource.Self")
                if (state.getEntity(sourceId) != null) listOf(sourceId) else emptyList()
            }

            is CardSource.TriggeringEntity -> {
                // The entity that fired the trigger ("it"); single-element when still in play.
                // Mirrors EffectTarget.TriggeringEntity for non-targeted gather → move pipelines
                // (Norin, Swift Survivalist: exile the just-blocked creature you control).
                val triggeringId = context.triggeringEntityId
                if (triggeringId != null && state.getEntity(triggeringId) != null) {
                    listOf(triggeringId)
                } else {
                    emptyList()
                }
            }

            is CardSource.LastKnownCombatPairedWithSource -> {
                // CR 509 combat pairing captured when the source left the battlefield. Restrict
                // to creatures still on the battlefield — a creature that already left can't be
                // affected (last-known-information identifies them, it doesn't resurrect them).
                val battlefield = state.getBattlefield().toSet()
                (context.triggerLastKnownBlockingOrBlockedByIds ?: emptyList())
                    .filter { it in battlefield }
            }

            is CardSource.CreaturesThatSaddledSource -> {
                // CR 702.171c — the creatures tapped to saddle this Mount this turn, recorded on
                // the source's CrewSaddleContributorsComponent. Restrict to creatures still on the
                // battlefield; one that already left can't be exiled/returned.
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for CreaturesThatSaddledSource")
                val battlefield = state.getBattlefield().toSet()
                state.getEntity(sourceId)
                    ?.get<CrewSaddleContributorsComponent>()
                    ?.creatureIds
                    ?.filter { it in battlefield }
                    ?: emptyList()
            }

            is CardSource.LastKnownEquipmentAttachedToSource -> {
                // CR 113.7a — the Equipment attached to the source captured before a self-sacrifice /
                // self-exile cost moved it off the battlefield. Restrict to permanents still on the
                // battlefield that are still Equipment: one that has since left (or stopped being an
                // Equipment) can't be attached. Last-known info identifies them, it doesn't resurrect
                // them or change what they currently are.
                val battlefield = state.getBattlefield().toSet()
                context.lastKnownSourceAttachments.filter { id ->
                    id in battlefield &&
                        state.projectedState.getSubtypes(id)
                            .any { it.equals("Equipment", ignoreCase = true) }
                }
            }

            is CardSource.EnteredViaThisResolution -> {
                // Permanents this resolving spell/ability put onto the battlefield, identified by the
                // EnteredViaAbilityComponent stamp (markEnteredViaSourceAbility) referencing this
                // source. Reads live battlefield state, so it survives the pauses of a multi-step
                // resolution and a RepeatDynamicTimes body (Valgavoth's Onslaught).
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for EnteredViaThisResolution")
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.EnteredViaAbilityComponent>()
                        ?.sourceId == sourceId
                }
            }
        }

        val cards = gathered

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
            // Per-card owners, so a multi-player reveal (e.g. each player reveals their top card)
            // can be attributed card-by-card in the UI. Only meaningful when owners differ.
            val cardOwnerIds = cards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.ownerId ?: context.controllerId
            }
            listOf(
                CardsRevealedEvent(
                    revealingPlayerId = context.controllerId,
                    cardIds = cards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName,
                    cardOwnerIds = if (cardOwnerIds.distinct().size > 1) cardOwnerIds else emptyList()
                )
            )
        } else {
            emptyList()
        }

        // Persist reveals for cards that came from a library source.
        // - Public reveal (`revealed = true`) → revealed to every player.
        // - Private look (Scry / Surveil / look-at-top-N) → revealed to the audience named by
        //   [GatherCardsEffect.lookAudience]: the controller by default, an opponent for "they
        //   look at the top N of your library" (Sauron's Ransom), or no one when a downstream
        //   decision is the only window into the cards.
        // HAND is intentionally excluded: the owner already sees their own hand, and the caster
        // should never automatically see another player's hand from a gather. Cards that genuinely
        // reveal/look at an opponent's hand use [RevealHandEffect] or [LookAtTargetHandEffect]
        // as an explicit prior step.
        val revealAudience: Set<EntityId> = when {
            effect.revealed -> state.turnOrder.toSet()
            isLibrarySource(effect.source) -> when (effect.lookAudience) {
                LookAudience.Controller -> setOf(context.controllerId)
                LookAudience.Opponent -> state.getOpponents(context.controllerId).toSet()
                LookAudience.None -> emptySet()
            }
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

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): com.wingedsheep.sdk.model.EntityId? =
        TargetResolutionUtils.resolvePlayerRef(player, context, state) ?: context.controllerId

    /**
     * Resolve a [Player] reference to the list of player ids whose zone should be gathered
     * from. Multi-player references like [Player.Each] / [Player.ActivePlayerFirst] /
     * [Player.EachOpponent] fan out (turn-order; opponents in turn-order minus controller),
     * so a single [CardSource.FromZone] / [CardSource.FromMultipleZones] / [CardSource.TopOfLibrary]
     * can gather "each player's graveyard/hand/library" at once. Single-player references
     * collapse to a one-element list.
     */
    private fun resolvePlayers(
        player: Player,
        context: EffectContext,
        state: GameState
    ): List<com.wingedsheep.sdk.model.EntityId>? = when (player) {
        is Player.Each, is Player.ActivePlayerFirst -> state.turnOrder
        is Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
        else -> resolvePlayer(player, context, state)?.let { listOf(it) }
    }
}
