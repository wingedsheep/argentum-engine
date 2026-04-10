package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.TurnTracker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.math.max
import kotlin.math.min

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

            is DynamicAmount.YourLifeTotal -> {
                state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: 0
            }

            is DynamicAmount.LifeTotal -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
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

            // Context-based values (kept — these read from trigger event context, not entity properties)
            is DynamicAmount.TriggerDamageAmount -> context.triggerDamageAmount ?: 0
            is DynamicAmount.TriggerLifeGainAmount -> context.triggerDamageAmount ?: 0
            is DynamicAmount.TriggerLifeLossAmount -> context.triggerDamageAmount ?: 0
            is DynamicAmount.LastKnownCounterCount -> context.triggerCounterCount ?: 0

            is DynamicAmount.CardTypesInLinkedExile -> {
                val sourceId = context.sourceId ?: return 0
                val linkedExile = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                    ?: return 0
                val cardTypes = mutableSetOf<com.wingedsheep.sdk.core.CardType>()
                for (exiledId in linkedExile.exiledIds) {
                    val card = state.getEntity(exiledId)
                        ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                        ?: continue
                    cardTypes.addAll(card.typeLine.cardTypes)
                }
                cardTypes.size
            }

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

            is DynamicAmount.AdditionalCostExiledCount -> context.exiledCardCount

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context) else evaluate(state, amount.ifFalse, context)
            }

            // Composable entity property — replaces SourcePower, TargetPower, CountersOnSelf, etc.
            is DynamicAmount.EntityProperty -> {
                val entityId = resolveEntityId(amount.entity, context) ?: return 0
                // Sacrificed entities already left battlefield — don't try projected state
                val useProjected = amount.entity !is EntityReference.Sacrificed
                resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected)
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
            is EntityReference.Sacrificed -> context.sacrificedPermanents.getOrNull(ref.index)
            is EntityReference.TappedAsCost -> context.tappedPermanents.getOrNull(ref.index)
            is EntityReference.Triggering -> context.triggeringEntityId
            is EntityReference.AffectedEntity -> context.affectedEntityId
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
        useProjected: Boolean
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
        }
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
