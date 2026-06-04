package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsStationUsingToughnessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.TurnTracker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.math.max
import kotlin.math.min

private val BASIC_LAND_SUBTYPES: Set<String> = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

/**
 * Evaluates DynamicAmount values against the current game state.
 *
 * DynamicAmount represents values that depend on game state, like
 * "the number of creatures you control" or "your life total".
 */
class DynamicAmountEvaluator(
    private val conditionEvaluator: ConditionEvaluator? = null,
    /**
     * Projection used for battlefield reads when the caller doesn't pass one. Default reads
     * the canonical [GameState.projectedState]; the [com.wingedsheep.engine.mechanics.layers.StateProjector]
     * swaps in an empty projection so its internal evaluator never re-enters its own lazy
     * initializer. Only invoked from branches that actually need projection — never eagerly
     * at function entry, otherwise resolution-time `Compare` conditions evaluated mid-layer
     * would recurse.
     */
    private val defaultProjection: (GameState) -> ProjectedState = { it.projectedState }
) {

    /**
     * Resolve the projection for branches that need battlefield reads. Lazy by design:
     * called only inside aggregate / entity-property branches, never at evaluator entry.
     */
    private fun resolveProjection(state: GameState, explicit: ProjectedState?): ProjectedState =
        explicit ?: defaultProjection(state)

    /**
     * Evaluate a DynamicAmount to get an actual integer value.
     *
     * @param projectedState Optional pre-computed projected state for battlefield reads.
     *   When provided, takes priority over [defaultProjection]. Mid-projection callers
     *   ([com.wingedsheep.engine.mechanics.layers.EffectApplicator]) pass their intermediate
     *   snapshot so layer-by-layer changes are visible to nested aggregates.
     */
    fun evaluate(
        state: GameState,
        amount: DynamicAmount,
        context: EffectContext,
        projectedState: ProjectedState? = null
    ): Int {
        return when (amount) {
            is DynamicAmount.Fixed -> amount.amount

            is DynamicAmount.XValue -> context.xValue ?: 0

            is DynamicAmount.TotalManaSpent -> context.totalManaSpent

            is DynamicAmount.ManaSpentOnX -> context.manaSpentOnXByColor[amount.color] ?: 0

            is DynamicAmount.YourLifeTotal -> {
                state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: 0
            }

            is DynamicAmount.LifeTotal -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
            }

            is DynamicAmount.StartingLifeTotal -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.getEntity(playerId)?.get<PlayerComponent>()?.startingLifeTotal ?: 20
            }

            is DynamicAmount.VariableReference -> {
                val name = amount.variableName
                if (name.endsWith("_count")) {
                    val collectionName = name.removeSuffix("_count")
                    context.pipeline.storedCollections[collectionName]?.size ?: 0
                } else {
                    context.pipeline.storedNumbers[name] ?: 0
                }
            }

            is DynamicAmount.StoredCardManaValue -> {
                val cards = context.pipeline.storedCollections[amount.collectionName] ?: return 0
                val cardId = cards.firstOrNull() ?: return 0
                state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0
            }

            is DynamicAmount.DistinctEntitiesInCollections -> {
                amount.collections
                    .flatMap { context.pipeline.storedCollections[it].orEmpty() }
                    .distinct()
                    .size
            }

            // Math operations — propagate [projectedState] so a mid-projection caller's
            // intermediate snapshot survives nested aggregates.
            is DynamicAmount.Add ->
                evaluate(state, amount.left, context, projectedState) + evaluate(state, amount.right, context, projectedState)

            is DynamicAmount.Subtract ->
                evaluate(state, amount.left, context, projectedState) - evaluate(state, amount.right, context, projectedState)

            is DynamicAmount.Multiply ->
                evaluate(state, amount.amount, context, projectedState) * amount.multiplier

            is DynamicAmount.IfPositive ->
                max(0, evaluate(state, amount.amount, context, projectedState))

            is DynamicAmount.Max ->
                max(evaluate(state, amount.left, context, projectedState), evaluate(state, amount.right, context, projectedState))

            is DynamicAmount.Min ->
                min(evaluate(state, amount.left, context, projectedState), evaluate(state, amount.right, context, projectedState))

            is DynamicAmount.ContextProperty -> evaluateContextProperty(state, amount.key, context)

            // Battlefield / zone aggregates — propagate the explicit snapshot (may be null);
            // each helper decides whether it actually needs to reach for [defaultProjection].
            // Doing this eagerly here would touch [state.projectedState] for non-battlefield
            // zones (e.g., GRAVEYARD `Count`), which infinitely recurses if a mid-projection
            // caller (a `ConditionalStaticAbility` condition evaluated inside [EffectApplicator])
            // reaches this branch through a default-constructed [ConditionEvaluator].
            is DynamicAmount.Count ->
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context, projectedState)

            is DynamicAmount.AggregateBattlefield ->
                evaluateBattlefieldAggregate(state, amount, context, projectedState)

            is DynamicAmount.AggregateZone ->
                evaluateZoneAggregate(state, amount, context, projectedState)

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context, projectedState)
                else evaluate(state, amount.ifFalse, context, projectedState)
            }

            is DynamicAmount.CountPlayersWith -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val playerIds = resolveUnifiedPlayerIds(state, amount.scope, context)
                playerIds.count { playerId ->
                    eval.evaluate(state, amount.condition, context.copy(controllerId = playerId))
                }
            }

            // Composable entity property — replaces SourcePower, TargetPower, CountersOnSelf, etc.
            is DynamicAmount.EntityProperty -> {
                val entityId = TargetResolutionUtils.resolveEntityReference(amount.entity, context, state)
                // Enchanted-creature power reads use last-known information when the source aura
                // has detached: the enchanted creature (and the aura) can leave the battlefield
                // before the ability resolves — e.g. removed in response to the aura's ETB
                // trigger — and "deals damage equal to its power" must use the power as it last
                // existed on the battlefield (CR 608.2g). Captured at trigger time.
                if (amount.entity is EntityReference.EnchantedCreature &&
                    amount.numericProperty is EntityNumericProperty.Power &&
                    (entityId == null || entityId !in state.getBattlefield())
                ) {
                    return context.enchantedCreatureLastKnownPower ?: 0
                }
                if (entityId == null) return 0
                // Sacrificed entities already left battlefield — consult the P/T snapshot
                // captured at sacrifice time (Rule 112.7a / 608.2h — "as it last existed
                // on the battlefield") before falling through to base stats.
                if (amount.entity is EntityReference.Sacrificed) {
                    val snapshot = context.sacrificedPermanents.firstOrNull { it.entityId == entityId }
                    when (amount.numericProperty) {
                        is EntityNumericProperty.Power ->
                            snapshot?.power?.let { return it }
                        is EntityNumericProperty.Toughness ->
                            snapshot?.toughness?.let { return it }
                        else -> { /* fall through */ }
                    }
                    return resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = false)
                }
                // Cost-storage reads of Power/Toughness mirror the Sacrificed path — the
                // chosen entity may have left the battlefield (or never been on it, for an
                // exile-zone pick) between cost payment and resolution (Rule 112.7a).
                // Resolution order: live projected value if still on battlefield → snapshot
                // captured at cost-pay time → base stats.
                if (amount.entity is EntityReference.FromCostStorage) {
                    val onBattlefield = entityId in state.getBattlefield()
                    when (amount.numericProperty) {
                        is EntityNumericProperty.Power,
                        is EntityNumericProperty.Toughness -> {
                            if (!onBattlefield) {
                                val snapshot = context.chosenEntitySnapshots.firstOrNull { it.entityId == entityId }
                                val snapVal = when (amount.numericProperty) {
                                    is EntityNumericProperty.Power -> snapshot?.power
                                    is EntityNumericProperty.Toughness -> snapshot?.toughness
                                    else -> null
                                }
                                if (snapVal != null) return snapVal
                                return resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = false)
                            }
                        }
                        else -> { /* fall through to projected path */ }
                    }
                    return resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = true, explicitProjected = projectedState)
                }
                // Tap-as-cost reads of Power/Toughness mirror the Sacrificed path — the tapped
                // permanent may have left the battlefield between cost payment and resolution
                // (Rule 112.7a). Power reads additionally get the Station-using-toughness
                // substitution: under Tapestry Warden, tapped creatures with toughness > power
                // contribute toughness to the cost-input formula instead of power.
                if (amount.entity is EntityReference.TappedAsCost) {
                    val snapshot = context.tappedPermanentSnapshots.firstOrNull { it.entityId == entityId }
                    val useSnapshot = snapshot != null && !state.getBattlefield().contains(entityId)
                    when (amount.numericProperty) {
                        is EntityNumericProperty.Power -> {
                            val power = if (useSnapshot) snapshot.power ?: 0
                                else resolveNumericProperty(state, entityId, EntityNumericProperty.Power, context, useProjected = true, explicitProjected = projectedState)
                            val toughness = if (useSnapshot) snapshot.toughness ?: 0
                                else resolveNumericProperty(state, entityId, EntityNumericProperty.Toughness, context, useProjected = true, explicitProjected = projectedState)
                            return if (toughness > power &&
                                controllerHasStationUsingToughness(state, entityId, snapshot?.controllerId)) toughness else power
                        }
                        is EntityNumericProperty.Toughness ->
                            if (useSnapshot) snapshot.toughness?.let { return it }
                        else -> { /* fall through */ }
                    }
                }
                resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = true, explicitProjected = projectedState)
            }

            is DynamicAmount.Divide -> {
                val num = evaluate(state, amount.numerator, context, projectedState)
                val den = evaluate(state, amount.denominator, context, projectedState)
                if (den == 0) return 0
                if (amount.roundUp) {
                    (num + den - 1) / den
                } else {
                    num / den
                }
            }

            is DynamicAmount.TurnTracking -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                when (amount.tracker) {
                    TurnTracker.CREATURES_DIED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.NONTOKEN_CREATURES_DIED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.OPPONENT_CREATURES_EXILED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.OPPONENTS_WHO_LOST_LIFE -> {
                        val controllerId = context.controllerId
                        state.turnOrder
                            .filter { it != controllerId }
                            .count { opponentId ->
                                state.getEntity(opponentId)
                                    ?.get<com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent>() != null
                            }
                    }
                    TurnTracker.DAMAGE_RECEIVED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent>()
                            ?.amount ?: 0
                    }
                    TurnTracker.LIFE_GAINED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent>()
                            ?.amount ?: 0
                    }
                    TurnTracker.LIFE_LOST -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent>() == true
                    }
                    TurnTracker.PLAYER_ATTACKED -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent>() == true
                    }
                    TurnTracker.DEALT_COMBAT_DAMAGE -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent>() == true
                    }
                    TurnTracker.COUNTERS_PUT_ON_CREATURE -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent>() == true
                    }
                    TurnTracker.LANDS_PLAYED -> playerIds.sumOf { playerId ->
                        val landDrops = state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LandDropsComponent>()
                            ?: return@sumOf 0
                        landDrops.maxPerTurn - landDrops.remaining
                    }
                    TurnTracker.LANDS_ENTERED_UNDER_CONTROL -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LandsEnteredUnderControlThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.FOOD_SACRIFICED -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent>() == true
                    }
                    TurnTracker.CARDS_LEFT_GRAVEYARD -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.DESCENDED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent>()
                            ?.count ?: 0
                    }
                }
            }

            is DynamicAmount.SpellsCastThisTurn -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                // excludeSelf drops the resolving spell's own record, matched by the spell's
                // stack entity id (CastSpellRecord.sourceEntityId == context.sourceId).
                val selfId = if (amount.excludeSelf) context.sourceId else null
                playerIds.sumOf { playerId ->
                    val records = state.spellsCastThisTurnByPlayer[playerId] ?: emptyList()
                    records.count { record ->
                        (selfId == null || record.sourceEntityId != selfId) &&
                            predicateEvaluator.matchesFilter(record, amount.filter)
                    }
                }
            }

            is DynamicAmount.CraftedMaterialsTotalPower -> {
                val sourceId = context.sourceId
                if (sourceId == null) 0 else {
                    val materials = state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent>()
                    if (materials == null) 0 else {
                        var total = 0
                        for (exiledId in materials.exiledIds) {
                            total += basePowerOfPrintedCard(state, exiledId)
                        }
                        total
                    }
                }
            }

        }
    }

    // =========================================================================
    // Context Property Evaluation
    // =========================================================================

    /**
     * Resolve a [ContextPropertyKey] against the current resolution [context].
     *
     * The trigger amount keys (damage / life-gained / life-lost) all read the same
     * `triggerDamageAmount` field — `LifeChangedEvent` populates it with the absolute
     * amount of life moved, regardless of direction.
     */
    private fun evaluateContextProperty(
        state: GameState,
        key: ContextPropertyKey,
        context: EffectContext
    ): Int = when (key) {
        ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT,
        ContextPropertyKey.PREVENTED_DAMAGE_AMOUNT,
        ContextPropertyKey.TRIGGER_LIFE_GAINED,
        ContextPropertyKey.TRIGGER_LIFE_LOST -> context.triggerDamageAmount ?: 0

        ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT,
        ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT -> context.triggerCounterCount ?: 0
        ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT -> context.triggerTotalCounterCount ?: 0

        ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT -> context.exiledCardCount
        ContextPropertyKey.ADDITIONAL_COST_BLIGHT_AMOUNT -> context.additionalCostBlightAmount

        ContextPropertyKey.TARGET_COUNT -> context.targets.size

        ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL -> context.triggerModesChosenCount ?: 0

        ContextPropertyKey.TRIGGER_SCRY_COUNT -> context.triggerScryCount ?: 0

        ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT -> context.triggerExcessDamageAmount ?: 0

        ContextPropertyKey.LINKED_EXILE_CARD_COUNT -> {
            val sourceId = context.sourceId
            if (sourceId == null) 0 else state.getEntity(sourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                ?.exiledIds?.size ?: 0
        }

        ContextPropertyKey.LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT -> {
            val sourceId = context.sourceId
            if (sourceId == null) 0 else {
                val linkedExile = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                if (linkedExile == null) 0 else {
                    val cardTypes = mutableSetOf<com.wingedsheep.sdk.core.CardType>()
                    for (exiledId in linkedExile.exiledIds) {
                        val card = state.getEntity(exiledId)?.get<CardComponent>() ?: continue
                        cardTypes.addAll(card.typeLine.cardTypes)
                    }
                    cardTypes.size
                }
            }
        }
    }

    // =========================================================================
    // Unified Filter Evaluation
    // =========================================================================

    private val predicateEvaluator = PredicateEvaluator()

    private fun controllerOf(state: GameState, projection: ProjectedState, entityId: EntityId): EntityId? =
        projection.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

    private fun evaluateUnifiedCount(
        state: GameState,
        player: Player,
        zone: Zone,
        filter: GameObjectFilter,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, player, context)
        val zoneType = resolveUnifiedZone(zone)
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Projection only matters for battlefield reads (type/subtype/color/keyword/P/T mutated
        // by continuous effects). Non-battlefield zones use base CardComponent via an empty
        // projection — and crucially do *not* reach for [defaultProjection], which during a
        // mid-projection caller would re-enter the lazy `state.projectedState` initializer.
        val projection: ProjectedState = if (zoneType == Zone.BATTLEFIELD) {
            resolveProjection(state, explicitProjection)
        } else {
            explicitProjection ?: ProjectedState(state, emptyMap())
        }

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == Zone.BATTLEFIELD) {
                state.getBattlefield().filter { controllerOf(state, projection, it) == playerId }
            } else {
                state.getZone(ZoneKey(playerId, zoneType))
            }
            entities.count { predicateEvaluator.matches(state, projection, it, filter, predicateContext) }
        }
    }

    /**
     * Evaluate an AggregateBattlefield: collect → filter → map → aggregate.
     */
    private fun evaluateBattlefieldAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateBattlefield,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projection = resolveProjection(state, explicitProjection)

        // "Self" is the affected entity when this aggregate is evaluated for a granted effect
        // (e.g. an Aura's "for each OTHER creature that shares a type with the enchanted creature":
        // self is the enchanted creature, not the Aura source). For a creature's own CDA there is
        // no affected entity, so it falls back to the source — the creature itself.
        val selfId = context.affectedEntityId ?: context.sourceId

        val matchingEntities = playerIds.flatMap { playerId ->
            state.getBattlefield()
                .filter { entityId ->
                    // Exclude self if requested (e.g., "other creatures you control")
                    if (amount.excludeSelf && entityId == selfId) return@filter false
                    controllerOf(state, projection, entityId) == playerId
                }
                .filter { entityId ->
                    predicateEvaluator.matches(state, projection, entityId, amount.filter, predicateContext)
                }
        }

        return when (amount.aggregation) {
            Aggregation.COUNT -> matchingEntities.size
            Aggregation.MAX -> {
                val prop = amount.property ?: return 0
                matchingEntities.maxOfOrNull { resolveCardNumericProperty(state, projection, it, prop) } ?: 0
            }
            Aggregation.MIN -> {
                val prop = amount.property ?: return 0
                matchingEntities.minOfOrNull { resolveCardNumericProperty(state, projection, it, prop) } ?: 0
            }
            Aggregation.SUM -> {
                val prop = amount.property ?: return 0
                matchingEntities.sumOf { resolveCardNumericProperty(state, projection, it, prop) }
            }
            Aggregation.DISTINCT_TYPES -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                projection.getTypes(entityId).ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }?.toSet()
                        ?: emptySet()
                }
            }.size
            Aggregation.DISTINCT_COLORS -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                projection.getColors(entityId).ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }
            }.size
            Aggregation.DISTINCT_NAMES -> matchingEntities.mapNotNullTo(mutableSetOf()) { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name
            }.size
            Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> matchingEntities.flatMapTo(mutableSetOf<String>()) { entityId ->
                projection.getSubtypes(entityId).ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                }.intersect(BASIC_LAND_SUBTYPES)
            }.size
            // Counters are physically stored on the permanent (base state, not layered), so read
            // CountersComponent directly. The map only holds kinds with a positive count
            // (CountersComponent.withRemoved drops the key at zero), so every key is a present kind.
            Aggregation.DISTINCT_COUNTER_TYPES -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                state.getEntity(entityId)?.get<CountersComponent>()?.counters?.keys ?: emptySet()
            }.size
        }
    }

    /**
     * Evaluate an AggregateZone: collect cards from a non-battlefield zone → filter → map → aggregate.
     */
    private fun evaluateZoneAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateZone,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Non-battlefield zone: avoid reaching for [defaultProjection] entirely. The predicate
        // evaluator falls back to base CardComponent for entries missing from the projection.
        val projection: ProjectedState = explicitProjection ?: ProjectedState(state, emptyMap())
        val matchingEntities = playerIds.flatMap { playerId ->
            state.getZone(ZoneKey(playerId, amount.zone))
                .filter { entityId ->
                    predicateEvaluator.matches(state, projection, entityId, amount.filter, predicateContext)
                }
        }

        // Aggregate
        return when (amount.aggregation) {
            Aggregation.COUNT -> matchingEntities.size
            Aggregation.MAX -> {
                val prop = amount.property ?: return 0
                matchingEntities.maxOfOrNull { resolveCardNumericProperty(state, null, it, prop) } ?: 0
            }
            Aggregation.MIN -> {
                val prop = amount.property ?: return 0
                matchingEntities.minOfOrNull { resolveCardNumericProperty(state, null, it, prop) } ?: 0
            }
            Aggregation.SUM -> {
                val prop = amount.property ?: return 0
                matchingEntities.sumOf { resolveCardNumericProperty(state, null, it, prop) }
            }
            Aggregation.DISTINCT_TYPES -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }?.toSet()
                        ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_COLORS -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_NAMES -> {
                matchingEntities.mapNotNullTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.name
                }.size
            }
            Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                matchingEntities.flatMapTo(mutableSetOf<String>()) { entityId ->
                    val subtypes: Set<String> = state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                    subtypes.intersect(BASIC_LAND_SUBTYPES)
                }.size
            }
            Aggregation.DISTINCT_COUNTER_TYPES -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CountersComponent>()?.counters?.keys ?: emptySet()
                }.size
            }
        }
    }

    private fun resolveUnifiedPlayerIds(
        state: GameState,
        player: Player,
        context: EffectContext
    ): List<EntityId> {
        return when (player) {
            is Player.You -> listOf(context.controllerId)
            is Player.Opponent -> state.turnOrder.filter { it != context.controllerId }
            is Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
            is Player.TargetOpponent -> listOfNotNull(context.opponentId)
            is Player.TargetPlayer -> listOfNotNull(context.opponentId)
            is Player.Each -> state.turnOrder
            is Player.Any -> state.turnOrder
            is Player.ContextPlayer -> {
                val target = context.targets.getOrNull(player.index) ?: return emptyList()
                when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> listOf(target.playerId)
                    else -> emptyList()
                }
            }
            is Player.ControllerOf -> {
                // Would need to resolve the target and find its controller
                emptyList()
            }
            is Player.OwnerOf -> {
                // Would need to resolve the target and find its owner
                emptyList()
            }
            is Player.TriggeringPlayer -> {
                listOfNotNull(context.triggeringEntityId)
            }
            is Player.ActivePlayerFirst -> {
                val activePlayer = state.activePlayerId ?: return state.turnOrder
                listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }
            }
        }
    }

    private fun resolveUnifiedZone(zone: Zone): Zone = zone

    private fun resolveCardNumericProperty(
        state: GameState,
        projected: ProjectedState?,
        entityId: EntityId,
        property: CardNumericProperty
    ): Int {
        return when (property) {
            CardNumericProperty.MANA_VALUE -> {
                val entity = state.getEntity(entityId)
                // Rule 202.3b: face-down permanents have mana value 0
                if (entity?.has<FaceDownComponent>() == true) 0
                else entity?.get<CardComponent>()?.manaValue ?: 0
            }
            CardNumericProperty.POWER -> {
                projected?.getPower(entityId)
                    ?: state.getEntity(entityId)?.get<CardComponent>()?.baseStats?.basePower
                    ?: 0
            }
            CardNumericProperty.TOUGHNESS -> {
                projected?.getToughness(entityId)
                    ?: state.getEntity(entityId)?.get<CardComponent>()?.baseStats?.baseToughness
                    ?: 0
            }
        }
    }

    // =========================================================================
    // Station Toughness Override
    // =========================================================================

    /**
     * Returns true if any permanent controlled by [entityId]'s controller has the
     * [GrantsStationUsingToughnessComponent], enabling the creature to station using
     * toughness instead of power. Reads projected controllers (Rule 613) so the
     * effect survives control-changing continuous effects on either permanent.
     *
     * [fallbackControllerId] is consulted when [entityId] is no longer on the battlefield
     * (projection has no controller) — typically the snapshot's last-known controller
     * captured at cost-payment time (Rule 112.7a).
     */
    private fun controllerHasStationUsingToughness(
        state: GameState,
        entityId: EntityId,
        fallbackControllerId: EntityId? = null
    ): Boolean {
        val projected = state.projectedState
        val controller = projected.getController(entityId) ?: fallbackControllerId ?: return false
        return state.getBattlefield().any { permanentId ->
            val perm = state.getEntity(permanentId) ?: return@any false
            perm.has<GrantsStationUsingToughnessComponent>() &&
                projected.getController(permanentId) == controller
        }
    }

    // =========================================================================
    // Entity Numeric Property Resolution
    // =========================================================================

    /**
     * Resolve a numeric property from an entity. Unified handler for [DynamicAmount.EntityProperty].
     */
    private fun resolveNumericProperty(
        state: GameState,
        entityId: EntityId,
        property: EntityNumericProperty,
        context: EffectContext,
        useProjected: Boolean,
        explicitProjected: ProjectedState? = null
    ): Int {
        return when (property) {
            is EntityNumericProperty.Power ->
                resolvePowerOrToughness(state, entityId, isPower = true, context, useProjected, explicitProjected)

            is EntityNumericProperty.Toughness ->
                resolvePowerOrToughness(state, entityId, isPower = false, context, useProjected, explicitProjected)

            is EntityNumericProperty.ManaValue ->
                state.getEntity(entityId)?.get<CardComponent>()?.manaValue ?: 0

            is EntityNumericProperty.ManaSpent -> {
                val spell = state.getEntity(entityId)?.get<SpellOnStackComponent>() ?: return 0
                spell.manaSpentWhite + spell.manaSpentBlue + spell.manaSpentBlack +
                    spell.manaSpentRed + spell.manaSpentGreen + spell.manaSpentColorless
            }

            is EntityNumericProperty.CounterCount -> {
                val countersComponent = state.getEntity(entityId)?.get<CountersComponent>() ?: return 0
                countersComponent.getCount(resolveCounterType(property.counterType))
            }

            is EntityNumericProperty.AttachmentCount ->
                state.getEntity(entityId)?.get<AttachmentsComponent>()?.attachedIds?.size ?: 0

            is EntityNumericProperty.BlockerCount ->
                state.getEntity(entityId)
                    ?.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()
                    ?.blockerIds?.size ?: 0

            // Read from projected state when available so layer-4 type-changing effects
            // (including Changeling) are honored. Falls back to base subtypes off the battlefield.
            is EntityNumericProperty.SubtypeCount ->
                resolveSubtypeCount(state, entityId, useProjected, explicitProjected)

            // Read from projected state when available so layer-5 color-changing effects are
            // honored. Falls back to the printed colors off the battlefield.
            is EntityNumericProperty.ColorCount ->
                resolveColorCount(state, entityId, useProjected, explicitProjected)
        }
    }

    private fun resolveSubtypeCount(
        state: GameState,
        entityId: EntityId,
        useProjected: Boolean,
        explicitProjected: ProjectedState?
    ): Int {
        if (useProjected) {
            val projectedSubtypes = resolveProjection(state, explicitProjected).getSubtypes(entityId)
            if (projectedSubtypes.isNotEmpty()) return projectedSubtypes.size
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.size ?: 0
    }

    private fun resolveColorCount(
        state: GameState,
        entityId: EntityId,
        useProjected: Boolean,
        explicitProjected: ProjectedState?
    ): Int {
        // For battlefield permanents the projected color set is authoritative even when empty —
        // a creature turned colorless by a layer-5 effect must count 0, not fall back to its
        // printed colors. Off the battlefield (or when projection isn't requested) use base colors.
        if (useProjected && entityId in state.getBattlefield()) {
            return resolveProjection(state, explicitProjected).getColors(entityId).size
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.colors?.size ?: 0
    }

    /**
     * Resolve power or toughness for an entity, using projected state when available
     * and falling back to base characteristic values.
     */
    private fun resolvePowerOrToughness(
        state: GameState,
        entityId: EntityId,
        isPower: Boolean,
        context: EffectContext,
        useProjected: Boolean,
        explicitProjected: ProjectedState? = null
    ): Int {
        if (useProjected) {
            val projection = resolveProjection(state, explicitProjected)
            val projectedValue = if (isPower) projection.getPower(entityId) else projection.getToughness(entityId)
            if (projectedValue != null) return projectedValue
        }
        // Last-known-info fallback for dies/leaves-the-battlefield triggers: when the
        // triggering entity is no longer on the battlefield, its projected P/T is gone,
        // so consult the value captured on the ZoneChangeEvent (Rule 603.10, 112.7a).
        if (entityId == context.triggeringEntityId || entityId == context.sourceId) {
            val lastKnown = if (isPower) context.triggerLastKnownPower else context.triggerLastKnownToughness
            if (lastKnown != null) return lastKnown
        }
        // Fall back to base stats (entity not on battlefield or projection disabled)
        return resolveCharacteristicValue(state, entityId, isPower, context)
    }

    /**
     * Resolve a characteristic value (power or toughness) from base stats.
     * Handles Fixed, Dynamic, and DynamicWithOffset characteristic values.
     */
    private fun resolveCharacteristicValue(
        state: GameState,
        entityId: EntityId,
        isPower: Boolean,
        context: EffectContext
    ): Int {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
        val value = if (isPower) card.baseStats?.power else card.baseStats?.toughness
        return when (value) {
            is CharacteristicValue.Fixed -> value.value
            is CharacteristicValue.Dynamic -> evaluate(state, value.source, context)
            is CharacteristicValue.DynamicWithOffset -> evaluate(state, value.source, context) + value.offset
            null -> 0
        }
    }

    /**
     * Read the printed base power of a card. Used by
     * [DynamicAmount.CraftedMaterialsTotalPower] to sum the power of cards in exile (CR 712.8a:
     * a card outside the battlefield/stack shows only its front-face characteristics, which is
     * the printed face for non-DFCs). Dynamic CDA values on the printed card are evaluated with
     * a fresh context targeting that card, falling back to 0 if anything is unset.
     */
    private fun basePowerOfPrintedCard(state: GameState, entityId: EntityId): Int {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
        val ctx = EffectContext(sourceId = entityId, controllerId = card.ownerId ?: return 0, opponentId = null)
        return when (val p = card.baseStats?.power) {
            is CharacteristicValue.Fixed -> p.value
            is CharacteristicValue.Dynamic -> evaluate(state, p.source, ctx)
            is CharacteristicValue.DynamicWithOffset -> evaluate(state, p.source, ctx) + p.offset
            null -> 0
        }
    }

    private fun resolveCounterType(filter: CounterTypeFilter): CounterType {
        return when (filter) {
            is CounterTypeFilter.Any -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.PlusOnePlusOne -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusOne -> CounterType.MINUS_ONE_MINUS_ONE
            is CounterTypeFilter.Loyalty -> CounterType.LOYALTY
            is CounterTypeFilter.Named -> {
                try {
                    CounterType.valueOf(filter.name.uppercase().replace(' ', '_'))
                } catch (_: IllegalArgumentException) {
                    CounterType.PLUS_ONE_PLUS_ONE
                }
            }
        }
    }
}
