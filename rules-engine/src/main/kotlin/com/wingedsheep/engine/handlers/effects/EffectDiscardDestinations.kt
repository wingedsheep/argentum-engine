package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.OptionalEffectDiscardDestination
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.effects.Effect

/** Prepare all choices against the pre-discard state; replay only the current move instruction. */
object EffectDiscardDestinations {
    const val UNDEFINED = "__undefinedDiscardCharacteristics"

    /** Unknown characteristics cannot satisfy a conjunction, but an independent OR branch can. */
    fun filterForUndefinedCharacteristics(filter: GameObjectFilter): GameObjectFilter? {
        if (filter.cardPredicates.isNotEmpty()) return null
        val states = filter.statePredicates.map { stateForUndefinedCharacteristics(it) ?: return null }
        val branches = filter.anyOf.mapNotNull(::filterForUndefinedCharacteristics)
        if (filter.anyOf.isNotEmpty() && branches.isEmpty()) return null
        return filter.copy(statePredicates = states, anyOf = branches)
    }

    private fun stateForUndefinedCharacteristics(predicate: StatePredicate): StatePredicate? {
        return when (predicate) {
            StatePredicate.HasManaAbility, is StatePredicate.HasActivatedAbilities, StatePredicate.HasMorphAbility, StatePredicate.HasDisguiseAbility,
            StatePredicate.HasGreatestPower, StatePredicate.HasLeastPower, StatePredicate.HasLeastPowerAmongAllCreatures,
            StatePredicate.HasGreatestManaValueAmongAllCreatures, StatePredicate.SharesNameWithSpellCastThisTurn,
            is StatePredicate.HasLeastManaValueAmong -> null
            is StatePredicate.And -> predicate.copy(predicates = predicate.predicates.map {
                stateForUndefinedCharacteristics(it) ?: return null
            })
            is StatePredicate.Or -> predicate.predicates.mapNotNull(::stateForUndefinedCharacteristics)
                .takeIf { it.isNotEmpty() }?.let { predicate.copy(predicates = it) }
            is StatePredicate.Not -> stateForUndefinedCharacteristics(predicate.predicate)
                ?.takeIf { it == predicate.predicate }?.let { predicate.copy(predicate = it) }
            else -> predicate
        }
    }

    fun recordUnknown(context: EffectContext, cards: List<EntityId>, vararg collections: String?): Map<String, List<EntityId>> =
        collections.filterNotNull().distinct().mapNotNull { collection ->
            val key = "$UNDEFINED:$collection"
            if (cards.isEmpty() && key !in context.pipeline.storedCollections) null else key to cards
        }.toMap()

    fun clearUnknown(context: EffectContext, collection: String): Map<String, List<EntityId>> {
        val key = "$UNDEFINED:$collection"
        return if (key in context.pipeline.storedCollections) mapOf(key to emptyList()) else emptyMap()
    }

    fun propagateUnknown(collections: Map<String, List<EntityId>>, prior: Map<String, List<EntityId>>, sourceCollection: String?): Map<String, List<EntityId>> {
        if (sourceCollection == null) return collections
        val unknown = prior["$UNDEFINED:$sourceCollection"].orEmpty().toSet()
        return collections + collections.filterKeys { !it.startsWith("$UNDEFINED:") }.mapNotNull { (name, ids) ->
            val key = "$UNDEFINED:$name"
            val hidden = ids.filter { it in unknown }
            if (hidden.isEmpty() && key !in prior) null else key to hidden
        }.toMap()
    }

    /**
     * The controller of the spell or ability that caused this discard instruction's discards: the
     * resolving effect's controller (inside a "for each player" loop `controllerId` is the iterated
     * discarder, so its real controller is `effectControllerId`), or for a cost discard the player
     * whose "unless" demand it pays ([EffectContext.discardCostCausedBy]; null for other costs).
     */
    fun causeControllerOf(context: EffectContext): EntityId? =
        if (context.discardIsCost) context.discardCostCausedBy else context.effectControllerId ?: context.controllerId

    /**
     * Pay an "unless [that player] discards …" demand with [cards] from [payerId]'s hand — ward's
     * discard, a pay-or-suffer discard (Perplex). The one discard path an effect discard takes, run
     * as a cost: madness and the discard bookkeeping apply, "if an effect causes you to discard"
     * replacements don't, and [demandedBy] is the discard's cause for the opponent-caused readers.
     * May pause for a destination choice; the caller propagates the pause.
     */
    fun payUnlessDiscard(
        registry: EffectExecutorRegistry,
        state: GameState,
        payerId: EntityId,
        cards: List<EntityId>,
        demandedBy: EntityId?,
        sourceId: EntityId?,
        origin: com.wingedsheep.engine.core.ProgramOrigin,
    ): EffectResult {
        if (cards.isEmpty()) return EffectResult.success(state)
        val paid = "unlessDiscardPayment"
        return registry.execute(
            state,
            com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect(
                from = paid,
                destination = com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = com.wingedsheep.sdk.scripting.effects.MoveType.Discard,
            ),
            EffectContext(
                sourceId = sourceId,
                controllerId = payerId,
                discardIsCost = true,
                discardCostCausedBy = demandedBy,
                programOrigin = origin,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf(paid to cards)),
            ),
        )
    }

    /** The player-facing button text: "Reveal it and put it on top of your library". */
    private fun optionLabel(choice: DiscardDestinationChoice): String {
        val destination = choice.destination
        val where = when (destination.zone) {
            Zone.LIBRARY -> when (destination.placement) {
                com.wingedsheep.sdk.scripting.effects.ZonePlacement.Bottom -> "on the bottom of your library"
                com.wingedsheep.sdk.scripting.effects.ZonePlacement.Shuffled -> "into your library and shuffle"
                else -> "on top of your library"
            }
            Zone.GRAVEYARD -> "into your graveyard"
            else -> "into ${destination.zone.displayName}"
        }
        return (if (choice.reveal) "Reveal it and put it " else "Put it ") + where
    }

    fun prepare(state: GameState, effect: Effect, context: EffectContext,
                cards: List<EntityId>, playerId: EntityId, zones: ZoneTransitionService): EffectResult? {
        val entries = ActiveReplacements.all(state).filter { it.granted || !state.projectedState.hasLostAllAbilities(it.sourceId) }.filter { it.effect is OptionalEffectDiscardDestination }
        if (entries.isEmpty()) return null
        val hand = state.getHand(playerId)
        val causeControllerId = causeControllerOf(context)
        for (id in cards) {
            if (id !in hand || id in context.discardDestinations) continue
            val candidates = entries.mapNotNull { entry ->
                val replacement = entry.effect as OptionalEffectDiscardDestination
                if (!zones.predicateEvaluator.matchesPlayer(state, state.projectedState, replacement.appliesTo.player, playerId, PredicateContext(controllerId = entry.controllerId, sourceId = entry.sourceId))) return@mapNotNull null
                // An "unless" payment is a cost: "if an effect causes you to discard" (Library of
                // Leng) skips it, but "if a spell or ability an opponent controls causes you to
                // discard" (Nephalia Academy) sees the discard that opponent's demand caused.
                if (context.discardIsCost && replacement.appliesTo.cause != ZoneChangeCause.DiscardedByOpponentEffect) return@mapNotNull null
                if (!ZoneTransitionService.discardCauseMatches(state, replacement.appliesTo.cause, causeControllerId, playerId, asCyclingCost = false)) return@mapNotNull null
                val filter = replacement.appliesTo.cardFilter
                if (filter != null && !zones.predicateEvaluator.matches(state, state.projectedState, id, filter,
                    PredicateContext(controllerId = entry.controllerId, sourceId = entry.sourceId))) return@mapNotNull null
                DiscardDestinationChoice(replacement.destination, replacement.reveal,
                    sourceName = state.getEntity(entry.sourceId)?.get<CardComponent>()?.name)
            }.distinctBy { it.destination to it.reveal }
            if (candidates.isEmpty()) continue
            val name = state.getEntity(id)?.get<CardComponent>()?.name ?: "this card"
            return EffectResult.from(state.suspendForDecision(
                question = { decisionId -> ChooseOptionDecision(
                    id = decisionId, playerId = playerId,
                    prompt = "Where do you want to discard $name?",
                    context = DecisionContext(sourceId = context.sourceId, phase = DecisionPhase.RESOLUTION),
                    options = candidates.map(::optionLabel) + "Use the normal discard destination",
                ) },
                answer = EffectDiscardDestinationContinuation(effect, context, id, candidates),
            ))
        }
        val libraryCards = cards.filter { context.discardDestinations[it]?.destination?.zone == Zone.LIBRARY }
        if (libraryCards.size > 1 && context.discardLibraryOrder == null) {
            return EffectResult.from(com.wingedsheep.engine.handlers.DecisionHandler().createCardSelectionDecision(
                state = state, playerId = playerId, sourceId = context.sourceId, sourceName = null,
                prompt = "Order discarded cards for your library (top card first)",
                options = libraryCards, minSelections = libraryCards.size, maxSelections = libraryCards.size,
                ordered = true, answer = EffectDiscardOrderContinuation(effect, context),
            ))
        }
        return null
    }
}
