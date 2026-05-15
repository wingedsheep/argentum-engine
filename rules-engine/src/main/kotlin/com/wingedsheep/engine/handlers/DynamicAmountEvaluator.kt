package com.wingedsheep.engine.handlers

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
     * When true, evaluateUnifiedCount will project state to correctly see
     * temporary type/subtype changes (e.g., BecomeCreatureType effects).
     * Set to false in StateProjector's internal evaluator to avoid infinite recursion.
     */
    private val projectForBattlefieldCounting: Boolean = true
) {

    /**
     * Evaluate a DynamicAmount to get an actual integer value.
     *
     * @param projectedState Optional pre-computed projected state for battlefield counting.
     *   When provided, this takes priority over auto-projection. Used by StateProjector
     *   to pass its intermediate projected state during CDA resolution.
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

            // Math operations
            is DynamicAmount.Add -> {
                evaluate(state, amount.left, context) + evaluate(state, amount.right, context)
            }

            is DynamicAmount.Subtract -> {
                evaluate(state, amount.left, context) - evaluate(state, amount.right, context)
            }

            is DynamicAmount.Multiply -> {
                evaluate(state, amount.amount, context) * amount.multiplier
            }

            is DynamicAmount.IfPositive -> {
                max(0, evaluate(state, amount.amount, context))
            }

            is DynamicAmount.Max -> {
                max(evaluate(state, amount.left, context), evaluate(state, amount.right, context))
            }

            is DynamicAmount.Min -> {
                min(evaluate(state, amount.left, context), evaluate(state, amount.right, context))
            }

            is DynamicAmount.ContextProperty -> evaluateContextProperty(state, amount.key, context)

            // Unified counting
            is DynamicAmount.Count -> {
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context, projectedState)
            }

            is DynamicAmount.AggregateBattlefield -> {
                evaluateBattlefieldAggregate(state, amount, context, projectedState)
            }

            is DynamicAmount.AggregateZone -> {
                evaluateZoneAggregate(state, amount, context)
            }

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context) else evaluate(state, amount.ifFalse, context)
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
                val entityId = resolveEntityId(amount.entity, context) ?: return 0
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
                            val power = if (useSnapshot) snapshot!!.power ?: 0
                                else resolveNumericProperty(state, entityId, EntityNumericProperty.Power, context, useProjected = true, explicitProjected = projectedState)
                            val toughness = if (useSnapshot) snapshot!!.toughness ?: 0
                                else resolveNumericProperty(state, entityId, EntityNumericProperty.Toughness, context, useProjected = true, explicitProjected = projectedState)
                            return if (toughness > power &&
                                controllerHasStationUsingToughness(state, entityId, snapshot?.controllerId)) toughness else power
                        }
                        is EntityNumericProperty.Toughness ->
                            if (useSnapshot) snapshot!!.toughness?.let { return it }
                        else -> { /* fall through */ }
                    }
                }
                resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = true, explicitProjected = projectedState)
            }

            is DynamicAmount.Divide -> {
                val num = evaluate(state, amount.numerator, context)
                val den = evaluate(state, amount.denominator, context)
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
                    TurnTracker.FOOD_SACRIFICED -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent>() == true
                    }
                    TurnTracker.CARDS_LEFT_GRAVEYARD -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent>()
                            ?.count ?: 0
                    }
                }
            }

            is DynamicAmount.CreaturesSharingTypeWithEntity -> {
                val entityId = resolveEntityId(amount.entity, context) ?: return 0
                val projected = if (projectForBattlefieldCounting) state.projectedState else null

                val entitySubtypes = if (projected != null) {
                    projected.getSubtypes(entityId)
                } else {
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
                    card.typeLine.subtypes.map { it.value }.toSet()
                }
                if (entitySubtypes.isEmpty()) return 0

                // Count all other creatures on the battlefield that share at least one subtype
                state.getBattlefield().count { otherId ->
                    if (otherId == entityId) return@count false
                    val isCreature = if (projected != null) {
                        "CREATURE" in projected.getTypes(otherId)
                    } else {
                        state.getEntity(otherId)?.get<CardComponent>()?.typeLine?.isCreature ?: false
                    }
                    if (!isCreature) return@count false
                    val subtypes = projected?.getSubtypes(otherId)
                        ?: state.getEntity(otherId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: return@count false
                    subtypes.any { it in entitySubtypes }
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
        ContextPropertyKey.TRIGGER_LIFE_GAINED,
        ContextPropertyKey.TRIGGER_LIFE_LOST -> context.triggerDamageAmount ?: 0

        ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT,
        ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT -> context.triggerCounterCount ?: 0
        ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT -> context.triggerTotalCounterCount ?: 0

        ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT -> context.exiledCardCount
        ContextPropertyKey.ADDITIONAL_COST_BLIGHT_AMOUNT -> context.additionalCostBlightAmount

        ContextPropertyKey.TARGET_COUNT -> context.targets.size

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

    private fun evaluateUnifiedCount(
        state: GameState,
        player: Player,
        zone: Zone,
        filter: GameObjectFilter,
        context: EffectContext,
        explicitProjectedState: ProjectedState? = null
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, player, context)
        val zoneType = resolveUnifiedZone(zone)

        val predicateContext = PredicateContext.fromEffectContext(context)

        // Use projected state for battlefield counting to see temporary type changes
        // (e.g., BecomeCreatureType effects). Use explicit projected state if provided,
        // otherwise auto-project when projectForBattlefieldCounting is enabled.
        val projected = if (zoneType == Zone.BATTLEFIELD) {
            explicitProjectedState ?: if (projectForBattlefieldCounting) {
                state.projectedState
            } else null
        } else null

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == Zone.BATTLEFIELD) {
                // Battlefield is shared, filter by controller
                // Use projected controller to account for control-changing effects
                state.getBattlefield().filter { entityId ->
                    val controllerId = projected?.getController(entityId)
                        ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
                    controllerId == playerId
                }
            } else {
                state.getZone(ZoneKey(playerId, zoneType))
            }

            entities.count { entityId ->
                if (projected != null) {
                    predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
                } else {
                    predicateEvaluator.matches(state, entityId, filter, predicateContext)
                }
            }
        }
    }

    /**
     * Evaluate an AggregateBattlefield: collect → filter → map → aggregate.
     */
    private fun evaluateBattlefieldAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateBattlefield,
        context: EffectContext,
        explicitProjectedState: ProjectedState? = null
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)

        val projected = explicitProjectedState ?: if (projectForBattlefieldCounting) {
            state.projectedState
        } else null

        // Collect and filter matching entities
        val matchingEntities = playerIds.flatMap { playerId ->
            state.getBattlefield()
                .filter { entityId ->
                    // Exclude self if requested (e.g., "other creatures you control")
                    if (amount.excludeSelf && entityId == context.sourceId) return@filter false
                    val controllerId = projected?.getController(entityId)
                        ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
                    controllerId == playerId
                }
                .filter { entityId ->
                    if (projected != null) {
                        predicateEvaluator.matchesWithProjection(state, projected, entityId, amount.filter, predicateContext)
                    } else {
                        predicateEvaluator.matches(state, entityId, amount.filter, predicateContext)
                    }
                }
        }

        // Aggregate
        return when (amount.aggregation) {
            Aggregation.COUNT -> matchingEntities.size
            Aggregation.MAX -> {
                val prop = amount.property ?: return 0
                matchingEntities.maxOfOrNull { resolveCardNumericProperty(state, projected, it, prop) } ?: 0
            }
            Aggregation.MIN -> {
                val prop = amount.property ?: return 0
                matchingEntities.minOfOrNull { resolveCardNumericProperty(state, projected, it, prop) } ?: 0
            }
            Aggregation.SUM -> {
                val prop = amount.property ?: return 0
                matchingEntities.sumOf { resolveCardNumericProperty(state, projected, it, prop) }
            }
            Aggregation.DISTINCT_TYPES -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    projected?.getTypes(entityId)
                        ?: state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }?.toSet()
                        ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_COLORS -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    projected?.getColors(entityId)
                        ?: state.getEntity(entityId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
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
                    val subtypes: Set<String> = projected?.getSubtypes(entityId)
                        ?: state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                    subtypes.intersect(BASIC_LAND_SUBTYPES)
                }.size
            }
        }
    }

    /**
     * Evaluate an AggregateZone: collect cards from a non-battlefield zone → filter → map → aggregate.
     */
    private fun evaluateZoneAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateZone,
        context: EffectContext
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Collect and filter matching entities from the zone
        val matchingEntities = playerIds.flatMap { playerId ->
            state.getZone(ZoneKey(playerId, amount.zone))
                .filter { entityId ->
                    predicateEvaluator.matches(state, entityId, amount.filter, predicateContext)
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
    // Entity Reference Resolution
    // =========================================================================

    /**
     * Resolve an [EntityReference] to an [EntityId] using the current effect context.
     */
    private fun resolveEntityId(ref: EntityReference, context: EffectContext): EntityId? =
        when (ref) {
            is EntityReference.Source -> context.sourceId
            is EntityReference.Target -> {
                val target = context.targets.getOrNull(ref.index)
                when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> target.playerId
                    else -> null
                }
            }
            is EntityReference.Sacrificed -> context.sacrificedPermanents.getOrNull(ref.index)?.entityId
            is EntityReference.TappedAsCost -> context.tappedPermanents.getOrNull(ref.index)
            is EntityReference.Triggering -> context.triggeringEntityId
            is EntityReference.AffectedEntity -> context.affectedEntityId
            is EntityReference.IterationEntity -> context.pipeline.iterationTarget
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
                resolvePowerOrToughness(state, entityId, isPower = true, context, useProjected)

            is EntityNumericProperty.Toughness ->
                resolvePowerOrToughness(state, entityId, isPower = false, context, useProjected)

            is EntityNumericProperty.ManaValue ->
                state.getEntity(entityId)?.get<CardComponent>()?.manaValue ?: 0

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
        }
    }

    private fun resolveSubtypeCount(
        state: GameState,
        entityId: EntityId,
        useProjected: Boolean,
        explicitProjected: ProjectedState?
    ): Int {
        if (useProjected) {
            val projected = explicitProjected
                ?: if (projectForBattlefieldCounting) state.projectedState else null
            if (projected != null) {
                val projectedSubtypes = projected.getSubtypes(entityId)
                if (projectedSubtypes.isNotEmpty()) return projectedSubtypes.size
            }
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.size ?: 0
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
        useProjected: Boolean
    ): Int {
        val projected = if (useProjected && projectForBattlefieldCounting) state.projectedState else null
        if (projected != null) {
            val projectedValue = if (isPower) projected.getPower(entityId) else projected.getToughness(entityId)
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
