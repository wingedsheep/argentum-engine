package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.KeywordActionOrderContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.isPlayerControlledByEffect
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
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.battlefield.entitiesChoice
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.entityIds
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.engine.view.Visibility
import kotlin.reflect.KClass

/**
 * Executor for GatherCardsEffect.
 *
 * Gathers cards from a source and stores them in a named collection
 * via [ExecutionResult.updatedCollections]. The cards are NOT removed
 * from their current zone — they are only referenced for subsequent
 * pipeline steps (SelectFromCollection, MoveCollection).
 */
class GatherCardsExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    /**
     * Who may look under a face-down card in exile — see [matchesVisibleExileCard]. Without a
     * registry (pure-data test wiring) a face-down exiled card never matches a filter.
     */
    cardRegistry: CardRegistry? = null,
) : EffectExecutor<GatherCardsEffect> {

    private val visibility = cardRegistry?.let { Visibility(it, conditionEvaluator = predicateEvaluator.conditions) }

    override val effectType: KClass<GatherCardsEffect> = GatherCardsEffect::class

    private val amountEvaluator = predicateEvaluator.amounts

    override fun execute(
        state: GameState,
        effect: GatherCardsEffect,
        context: EffectContext
    ): EffectResult {
        // The cards a filter read to build this pool (#3340): a filtered gather over a hand or
        // library has a size nobody else can know. See [HiddenPools].
        var filterRead: List<EntityId> = emptyList()
        var inheritedDeciders: List<EntityId> = emptyList()
        val gathered = when (val source = effect.source) {
            is CardSource.TopOfLibrary -> {
                val count = amountEvaluator.evaluate(state, source.count, context)
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                // For a mill, scry or surveil, apply ModifyKeywordActionAmount replacement effects
                // to the announced count per acting player — "mill that many plus four instead",
                // "mill twice that many instead" (CR 701.17), "scry that many cards plus one
                // instead" (CR 701.22), "look at an additional two cards" (CR 701.25b).
                val action = when {
                    source.isMill -> KeywordActionAmountModifier.Action.MILL
                    source.isScry -> KeywordActionAmountModifier.Action.SCRY
                    source.isSurveil -> KeywordActionAmountModifier.Action.SURVEIL
                    else -> null
                }
                val effectiveCounts = if (action == null) playerIds.associateWith { count } else {
                    val outcomes = playerIds.associateWith {
                        KeywordActionAmountModifier.outcomes(state, it, count, predicateEvaluator, action)
                    }
                    askReplacementOrder(state, effect, context, action, outcomes)?.let { return it }
                    outcomes.mapValues { (playerId, options) ->
                        context.keywordActionCounts[playerId]?.takeIf { decided -> options.any { it.count == decided } }
                            ?: options.singleOrNull()?.count
                            ?: return EffectResult.error(state, "No replacement order chosen for a ${action.verb.lowercase()}")
                    }
                }
                playerIds.flatMap { playerId ->
                    state.getZone(ZoneKey(playerId, Zone.LIBRARY)).take(effectiveCounts.getValue(playerId))
                }
            }

            is CardSource.BottomOfLibrary -> {
                val count = amountEvaluator.evaluate(state, source.count, context)
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                playerIds.flatMap { playerId ->
                    state.getZone(ZoneKey(playerId, Zone.LIBRARY)).takeLast(count.coerceAtLeast(0))
                }
            }

            is CardSource.FromZone -> {
                val playerIds = resolvePlayers(source.player, context, state)
                    ?: return EffectResult.error(state, "Could not resolve player for GatherCards")
                val allCards = playerIds.flatMap { playerId ->
                    if (source.zone == Zone.SIDEBOARD && state.isPlayerControlledByEffect(playerId)) emptyList()
                    else zoneCards(state, playerId, source.zone)
                }
                val filtered = if (source.filter != GameObjectFilter.Any) {
                    filterRead = allCards
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { cardId ->
                        matchesVisibleExileCard(state, cardId, source.zone, context) &&
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
                        if (zone == Zone.SIDEBOARD && state.isPlayerControlledByEffect(playerId)) emptyList()
                        else zoneCards(state, playerId, zone).map { it to zone }
                    }
                }
                if (source.filter != GameObjectFilter.Any) {
                    filterRead = allCards.map { it.first }
                    val predicateContext = PredicateContext.fromEffectContext(context)
                    allCards.filter { (cardId, zone) ->
                        matchesVisibleExileCard(state, cardId, zone, context) &&
                            predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
                    }.map { it.first }
                } else {
                    allCards.map { it.first }
                }
            }

            is CardSource.FromVariable -> {
                inheritedDeciders = HiddenPools.deciders(context.pipeline.storedCollections, source.variableName)
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

            // The permanents sacrificed this way (a cost or an earlier same-resolution sacrifice),
            // kept only while each is still in its owner's graveyard — a card that has moved on is
            // a new object (CR 400.7), and a token is gone entirely.
            is CardSource.SacrificedThisWay -> {
                context.sacrificedPermanents.mapNotNull { snapshot ->
                    val cardId = snapshot.entityId
                    val ownerId = state.getEntity(cardId)?.get<OwnerComponent>()?.playerId
                        ?: return@mapNotNull null
                    cardId.takeIf { it in state.getZone(ZoneKey(ownerId, Zone.GRAVEYARD)) }
                }.distinct()
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
                    state, baseFilter, predicateContext, excludeSelfId,
                    predicateEvaluator = predicateEvaluator
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
                //
                // State-aware resolution: the host is often a *relational* reference — "enchanted
                // creature" (Mark of Eviction's "all Auras attached to that creature") is read off
                // the source's own attachment link, which only the `state` overload knows how to
                // follow. The stateless overload returns null for those and the gather silently
                // yields nothing; it is a strict subset of this one, so every other host shape
                // resolves exactly as before.
                //
                // Last-known leg (CR 608.2h): once the triggering permanent has left the battlefield
                // — "whenever an equipped creature dies, attach all Equipment attached to that
                // creature" (Rhuk, Hexgold Nabber) — its links are gone, so the attachments frozen on
                // the trigger's zone change identify them. They still have to be on the battlefield
                // and match the filter now; last-known info names them, it doesn't resurrect them.
                //
                // Off-battlefield leg ([CardSource.AttachedTo.zone] ≠ battlefield): the same frozen
                // attachments, kept where they followed the host out of play — see
                // [lastKnownAttachmentsInZone].
                if (source.zone != Zone.BATTLEFIELD) {
                    lastKnownAttachmentsInZone(state, source, context)
                } else {
                    val hostId = context.resolveTarget(source.host, state)
                    val attachedIds = if (hostId != null && hostId in state.getBattlefield()) {
                        state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
                    } else if (source.host == com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity) {
                        context.triggerContext?.lastKnownAttachmentIds ?: emptyList()
                    } else {
                        emptyList()
                    }
                    if (attachedIds.isEmpty()) {
                        emptyList()
                    } else {
                        val matching = BattlefieldFilterUtils
                            .findMatchingOnBattlefield(state, source.filter, context, predicateEvaluator = predicateEvaluator)
                            .toSet()
                        attachedIds.filter { it in matching }
                    }
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
                    // One object may fill several instances of "target" (CR 115.3) — an artifact
                    // creature chosen as both "target artifact" and "target creature". A gathered
                    // collection is a set of objects, so it is visited, moved and counted once.
                }.distinct()
            }

            is CardSource.SourceLinkedBattlefield -> {
                SourceObjectRecords.gather(state, context, source.key)
            }

            is CardSource.FromLinkedExile -> {
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for FromLinkedExile")
                if (state.getEntity(sourceId) == null && context.sourceBattlefieldTimestamp == null) {
                    return EffectResult.error(state, "Source entity not found for FromLinkedExile")
                }
                val inExile = com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
                    .exiledCards(state, context)
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

            is CardSource.Entity -> {
                // The one entity the reference names now ("this card", "it", "enchanted creature"),
                // single-element while it still exists. The state-aware resolve reads relational
                // references — an Aura's host falls back to the host it last had (CR 608.2h).
                val id = context.resolveTarget(source.entity, state)
                if (id != null && state.getEntity(id) != null) listOf(id) else emptyList()
            }

            is CardSource.LastKnownCombatPairedWithSource -> {
                // CR 509 combat pairing captured when the source left the battlefield. Restrict
                // to creatures still on the battlefield — a creature that already left can't be
                // affected (last-known-information identifies them, it doesn't resurrect them).
                val battlefield = state.getBattlefield().toSet()
                (context.triggerContext?.lastKnownBlockingOrBlockedByIds ?: emptyList())
                    .filter { it in battlefield }
            }

            is CardSource.CreaturesThatCrewedOrSaddledSource -> {
                // CR 702.122 / 702.171c — the creatures tapped to crew or saddle the source this turn, recorded on
                // the source's CrewSaddleContributorsComponent. Restrict to creatures still on the
                // battlefield; one that already left can't be exiled/returned.
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for CreaturesThatCrewedOrSaddledSource")
                val battlefield = state.getBattlefield().toSet()
                state.getEntity(sourceId)
                    ?.get<CrewSaddleContributorsComponent>()
                    ?.creatureIds
                    ?.filter { it in battlefield }
                    ?: emptyList()
            }

            is CardSource.CreaturesThatConvokedSource -> {
                // CR 702.51c — every creature tapped for the source's convoke, in tap order, read off
                // the spell's record (or, for a permanent spell that has resolved, the cast-choices
                // bag it carried). Departed convokers are kept, because an instruction may still
                // apply to them through last-known information (CR 701.50b — connive).
                val source = context.sourceId?.let { state.getEntity(it) }
                    ?: return EffectResult.error(state, "No source entity for CreaturesThatConvokedSource")
                (source.entitiesChoice(ChoiceSlot.CONVOKED_CREATURES)?.entryStamps
                    ?: source.get<SpellOnStackComponent>()?.convokedCreatures)
                    ?.keys?.toList()
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

        // A forbidden library contributes nothing to the search (Shadow of Doubt: every library;
        // Ashiok, Dream Render: an opponent's own library when their own spell or ability makes
        // them search it — see LibrarySearchRestrictions). Only the library half is dropped:
        // "search your graveyard and/or library" still finds graveyard cards. The rest of the
        // instruction (move nothing, shuffle) still runs. The searcher is who the gather names —
        // the controller unless another player searches their own library (Sandworm's "its
        // controller may search their library"); the cause is the spell or ability's controller,
        // stable across a per-player iteration.
        val searcher = if (effect.search) resolvePlayer(effect.searcher, context, state) else null
        val causeController = context.effectControllerId ?: context.controllerId
        val forbiddenOwners = searcher
            ?.let { LibrarySearchRestrictions.forbiddenLibraryOwners(state, it, causeController) }
            .orEmpty()
        val libraries = forbiddenOwners.flatMapTo(HashSet()) { state.getZone(ZoneKey(it, Zone.LIBRARY)) }
        val cards = limitSearchDepth(state, searcher, gathered.filter { it !in libraries })
        // The search is no search at all when every library it looks through is forbidden, so its
        // searched event must not fire (EmitLibrarySearchedEventExecutor reads this).
        val searchForbidden = searcher != null && forbiddenOwners.isNotEmpty() &&
            LibrarySearchRestrictions.searchedLibraryOwners(effect.source) { resolvePlayers(it, context, state) }
                .let { owners -> owners != null && owners.isNotEmpty() && forbiddenOwners.containsAll(owners) }
        // A library nobody may search is publicly out of the pool, so its cards decide nothing.
        val poolDeciders = (inheritedDeciders +
            HiddenPools.hiddenOwners(state, filterRead.filter { it !in libraries }, visibility, context.pipeline.storedCollections)).distinct()
        // Published for the pipeline's EmitLibrarySearchedEventEffect tail, so the searched
        // event names this searcher.
        val searcherCollection = searcher
            ?.let {
                mapOf(
                    com.wingedsheep.engine.handlers.PipelineState.SEARCHER_KEY to listOf(it),
                    com.wingedsheep.engine.handlers.PipelineState.SEARCH_FORBIDDEN_KEY to
                        if (searchForbidden) listOf(it) else emptyList(),
                )
            }
            .orEmpty()

        // An empty pool still publishes its marks: a search that found nothing is still a search,
        // and a filtered hidden zone with no match is still a hidden fact.
        val events = if (effect.revealed && cards.isNotEmpty()) {
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
            // can be attributed card-by-card in the UI. Sent whenever some card isn't the
            // revealer's own — a clash reveals the chosen opponent's top card under the clasher's
            // id, and in multiplayer the client needs to know *which* player's card that is.
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
                    cardOwnerIds = if (cardOwnerIds.any { it != context.controllerId }) cardOwnerIds else emptyList()
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
        val newState = if (revealAudience.isNotEmpty() && cards.isNotEmpty()) {
            LibraryRevealUtils.markRevealed(state, cards, revealAudience)
        } else {
            state
        }

        // A search marks itself with its searcher, so the pick that follows can say it is one (a
        // found card is a gain; an empty pick is "fail to find"), even when it found nothing. A
        // later non-search gather into the same name clears a stale mark.
        val searchedKey = com.wingedsheep.engine.handlers.PipelineState.searchedKey(effect.storeAs)
        val searchCollections = when {
            effect.search -> mapOf(effect.storeAs to cards, searchedKey to listOfNotNull(searcher))
            searchedKey in context.pipeline.storedCollections -> mapOf(effect.storeAs to cards, searchedKey to emptyList())
            else -> mapOf(effect.storeAs to cards)
        }
        // A mill marks what it took, so the move that follows is reported as a mill (CR 701.17).
        val milledKey = com.wingedsheep.engine.handlers.PipelineState.milledKey(effect.storeAs)
        val collections = when {
            (effect.source as? CardSource.TopOfLibrary)?.isMill == true -> searchCollections + (milledKey to cards)
            milledKey in context.pipeline.storedCollections -> searchCollections + (milledKey to emptyList())
            else -> searchCollections
        }
        val sourceVariable = effect.source as? CardSource.FromVariable
        val updatedCollections = if (sourceVariable != null) {
            com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.propagateUnknown(
                collections, context.pipeline.storedCollections, sourceVariable.variableName)
        } else {
            collections + com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.clearUnknown(context, effect.storeAs)
        }
        val hiddenMarks = HiddenPools.marks(listOf(effect.storeAs), poolDeciders, context.pipeline.storedCollections)
        return EffectResult.success(newState, events).copy(updatedCollections = updatedCollections + hiddenMarks + searcherCollection)
    }


    /**
     * [CardSource.AttachedTo] with a non-battlefield [CardSource.AttachedTo.zone]: the triggering
     * host's last-known attachments (frozen on its battlefield exit) that are now in the
     * controller's [CardSource.AttachedTo.zone] and match the filter. Entity ids persist across
     * zones, so a card proves it is the very object that was attached by still carrying its own
     * battlefield-exit snapshot naming this host — the snapshot is stripped on its next zone
     * change, when it becomes a new object (CR 400.7).
     */
    private fun lastKnownAttachmentsInZone(
        state: GameState,
        source: CardSource.AttachedTo,
        context: EffectContext
    ): List<EntityId> {
        if (source.host != com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity) return emptyList()
        val hostId = context.triggeringEntityId ?: return emptyList()
        val frozen = context.triggerContext?.lastKnownAttachmentIds ?: return emptyList()
        val zoneCards = state.getZone(ZoneKey(context.controllerId, source.zone)).toSet()
        val predicateContext = PredicateContext.fromEffectContext(context)
        return frozen.filter { cardId ->
            cardId in zoneCards &&
                state.getEntity(cardId)?.get<LastKnownPermanentComponent>()?.snapshot?.attachedTo == hostId &&
                predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
        }
    }
    private fun isLibrarySource(source: CardSource): Boolean = when (source) {
        is CardSource.TopOfLibrary, is CardSource.BottomOfLibrary -> true
        is CardSource.FromZone -> source.zone == Zone.LIBRARY
        is CardSource.FromMultipleZones -> source.zones.any { it == Zone.LIBRARY }
        else -> false
    }

    /**
     * A player's cards in [zone]. The battlefield is nobody's zone: its "cards" of a player are the
     * permanents that player controls (projected — a stolen permanent counts for its controller),
     * matching how [CardSource.FromZone] / [CardSource.FromMultipleZones] describe it.
     */
    private fun zoneCards(state: GameState, playerId: EntityId, zone: Zone): List<EntityId> =
        if (zone == Zone.BATTLEFIELD) state.controlledBattlefield(playerId)
        else state.getZone(ZoneKey(playerId, zone))

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): com.wingedsheep.sdk.model.EntityId? =
        TargetResolutionUtils.resolvePlayerRef(player, context, state) ?: context.controllerId

    /**
     * "That player searches the top four cards of that library instead" (Aven Mindcensor, CR
     * 701.23f): when a [com.wingedsheep.sdk.scripting.LimitLibrarySearch] covers [searcher], a
     * library card is findable only if it is among the top N of its library. Non-library cards
     * (the graveyard half of "search your graveyard and library") are untouched.
     */
    private fun limitSearchDepth(state: GameState, searcher: EntityId?, gathered: List<EntityId>): List<EntityId> {
        if (searcher == null || gathered.isEmpty()) return gathered
        val depth = KeywordActionAmountModifier.librarySearchDepth(state, searcher, predicateEvaluator)
            ?: return gathered
        val libraries = state.turnOrder.map { state.getZone(ZoneKey(it, Zone.LIBRARY)) }
        val allLibraryCards = libraries.flatMapTo(HashSet()) { it }
        val searchable = libraries.flatMapTo(HashSet()) { it.take(depth) }
        return gathered.filter { it !in allLibraryCards || it in searchable }
    }

    /**
     * When the order of a player's [action] replacements changes the count (a doubler next to a
     * "+4") and they haven't picked one for this gather yet, ask them (CR 616.1 — the affected
     * player chooses; several players choose in APNAP order). Returns the pause, or null when every
     * count is settled.
     */
    private fun askReplacementOrder(
        state: GameState,
        effect: GatherCardsEffect,
        context: EffectContext,
        action: KeywordActionAmountModifier.Action,
        outcomes: Map<EntityId, List<KeywordActionAmountModifier.AmountOutcome>>,
    ): EffectResult? {
        if (outcomes.values.all { it.size == 1 }) return null
        val playerId = state.apnapOrder.firstOrNull { player ->
            val options = outcomes[player] ?: return@firstOrNull false
            options.size > 1 && options.none { it.count == context.keywordActionCounts[player] }
        } ?: return null
        val options = outcomes.getValue(playerId)
        return EffectResult.from(state.suspendForDecision(
            question = { decisionId -> ChooseOptionDecision(
                id = decisionId, playerId = playerId,
                prompt = KeywordActionAmountModifier.prompt(action),
                context = DecisionContext(sourceId = context.sourceId, phase = DecisionPhase.RESOLUTION),
                options = KeywordActionAmountModifier.optionLabels(action, options),
            ) },
            answer = KeywordActionOrderContinuation(effect, context, playerId, options.map { it.count }),
        ))
    }

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
        is Player.EachDefendingPlayer -> com.wingedsheep.engine.mechanics.combat.CombatDefenders.allDefendingPlayersInApnapOrder(state)
        is Player.EachAttackedPlayer -> com.wingedsheep.engine.mechanics.combat.CombatDefenders.attackedPlayersInApnapOrder(state)
        is Player.Each, is Player.ActivePlayerFirst -> state.turnOrder
        is Player.EachStartingWithYou -> state.turnOrderStartingWith(context.controllerId)
        is Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
        is Player.EachOtherPlayer -> state.getOtherPlayers(context.controllerId)
        // "Up to one target player" with no player chosen names nobody (Endurance) — never fall
        // back to the controller, whose own zone the effect would otherwise sweep.
        Player.TargetPlayer, Player.TargetOpponent, is Player.ContextPlayer ->
            listOfNotNull(TargetResolutionUtils.resolvePlayerRef(player, context, state))
        else -> resolvePlayer(player, context, state)?.let { listOf(it) }
    }

    /**
     * A filter reads a card's characteristics, and nobody can read those under a card exiled face
     * down unless an effect lets them look (foretell, "you may look at", a may-play permission —
     * the [Visibility] rules). So a face-down exiled card only matches a filtered gather when the
     * effect's controller may look at it: Kaya the Inexorable's emblem offers a foretold legendary
     * you own, never one an opponent's Gonti exiled face down. Every other card matches as before.
     */
    private fun matchesVisibleExileCard(state: GameState, cardId: EntityId, zone: Zone, context: EffectContext): Boolean {
        if (zone != Zone.EXILE || faceDownDisplayName(state, cardId) == null) return true
        val viewer = context.controllerId
        return visibility?.isCardIdentityVisibleTo(state, Zone.EXILE, cardId, viewer) ?: false
    }

}
