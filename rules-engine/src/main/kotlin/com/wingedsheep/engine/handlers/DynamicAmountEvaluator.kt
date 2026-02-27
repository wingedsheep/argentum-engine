package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
                    context.storedCollections[collectionName]?.size ?: 0
                } else {
                    context.storedNumbers[name] ?: 0
                }
            }

            is DynamicAmount.StoredCardManaValue -> {
                val cards = context.storedCollections[amount.collectionName] ?: return 0
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

            // Context-based values
            is DynamicAmount.SacrificedPermanentPower -> {
                val sacrificedId = context.sacrificedPermanents.firstOrNull() ?: return 0
                val card = state.getEntity(sacrificedId)?.get<CardComponent>() ?: return 0
                when (val power = card.baseStats?.power) {
                    is com.wingedsheep.sdk.model.CharacteristicValue.Fixed -> power.value
                    is com.wingedsheep.sdk.model.CharacteristicValue.Dynamic -> evaluate(state, power.source, context)
                    is com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset -> evaluate(state, power.source, context) + power.offset
                    null -> 0
                }
            }

            is DynamicAmount.TriggerDamageAmount -> context.triggerDamageAmount ?: 0

            is DynamicAmount.TriggerLifeGainAmount -> context.triggerDamageAmount ?: 0

            is DynamicAmount.SacrificedPermanentToughness -> {
                val sacrificedId = context.sacrificedPermanents.firstOrNull() ?: return 0
                val card = state.getEntity(sacrificedId)?.get<CardComponent>() ?: return 0
                when (val toughness = card.baseStats?.toughness) {
                    is com.wingedsheep.sdk.model.CharacteristicValue.Fixed -> toughness.value
                    is com.wingedsheep.sdk.model.CharacteristicValue.Dynamic -> evaluate(state, toughness.source, context)
                    is com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset -> evaluate(state, toughness.source, context) + toughness.offset
                    null -> 0
                }
            }

            // Other types - return 0 for unimplemented
            is DynamicAmount.CardTypesInAllGraveyards -> {
                // TODO: Count unique card types across all graveyards
                0
            }

            is DynamicAmount.ColorsAmongPermanentsYouControl -> {
                // TODO: Count unique colors
                0
            }

            // Unified counting
            is DynamicAmount.Count -> {
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context, projectedState)
            }

            is DynamicAmount.AggregateBattlefield -> {
                evaluateBattlefieldAggregate(state, amount, context, projectedState)
            }

            is DynamicAmount.SourcePower -> {
                val sourceId = context.sourceId ?: return 0
                val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return 0
                when (val power = card.baseStats?.power) {
                    is com.wingedsheep.sdk.model.CharacteristicValue.Fixed -> power.value
                    is com.wingedsheep.sdk.model.CharacteristicValue.Dynamic -> evaluate(state, power.source, context)
                    is com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset -> evaluate(state, power.source, context) + power.offset
                    null -> 0
                }
            }

            is DynamicAmount.CountersOnSelf -> {
                val sourceId = context.sourceId ?: return 0
                val countersComponent = state.getEntity(sourceId)?.get<CountersComponent>() ?: return 0
                val counterType = resolveCounterType(amount.counterType)
                countersComponent.getCount(counterType)
            }

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context) else evaluate(state, amount.ifFalse, context)
            }

            is DynamicAmount.TargetPower -> {
                val target = context.targets.getOrNull(amount.targetIndex) ?: return 0
                val targetEntityId = when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                    else -> return 0
                }
                // Use projected state for accurate power (accounts for continuous effects)
                val projected = if (projectForBattlefieldCounting) {
                    StateProjector(DynamicAmountEvaluator(projectForBattlefieldCounting = false))
                        .project(state)
                } else null
                if (projected != null) {
                    projected.getPower(targetEntityId) ?: 0
                } else {
                    val card = state.getEntity(targetEntityId)?.get<CardComponent>() ?: return 0
                    when (val power = card.baseStats?.power) {
                        is com.wingedsheep.sdk.model.CharacteristicValue.Fixed -> power.value
                        is com.wingedsheep.sdk.model.CharacteristicValue.Dynamic -> evaluate(state, power.source, context)
                        is com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset -> evaluate(state, power.source, context) + power.offset
                        null -> 0
                    }
                }
            }

            is DynamicAmount.CountersOnTarget -> {
                val target = context.targets.getOrNull(amount.targetIndex) ?: return 0
                val targetEntityId = when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                    else -> return 0
                }
                val countersComponent = state.getEntity(targetEntityId)?.get<CountersComponent>() ?: return 0
                val counterType = resolveCounterType(amount.counterType)
                countersComponent.getCount(counterType)
            }

            is DynamicAmount.TargetManaValue -> {
                val target = context.targets.getOrNull(amount.targetIndex) ?: return 0
                val spellEntityId = when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
                    else -> return 0
                }
                state.getEntity(spellEntityId)?.get<CardComponent>()?.manaValue ?: 0
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

            is DynamicAmount.CreaturesSharingTypeWithTriggeringEntity -> {
                val triggeringId = context.triggeringEntityId ?: return 0
                // Get the triggering creature's subtypes from projected state
                val projected = if (projectForBattlefieldCounting) {
                    com.wingedsheep.engine.mechanics.layers.StateProjector(
                        DynamicAmountEvaluator(projectForBattlefieldCounting = false)
                    ).project(state)
                } else null

                val triggeringSubtypes = if (projected != null) {
                    projected.getSubtypes(triggeringId)
                } else {
                    val card = state.getEntity(triggeringId)?.get<CardComponent>() ?: return 0
                    card.typeLine.subtypes.map { it.value }.toSet()
                }
                if (triggeringSubtypes.isEmpty()) return 0

                // Count creatures the controller controls that share at least one subtype
                state.getBattlefield().count { entityId ->
                    val controllerId = projected?.getController(entityId)
                        ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
                    if (controllerId != context.controllerId) return@count false
                    val isCreature = if (projected != null) {
                        "CREATURE" in projected.getTypes(entityId)
                    } else {
                        state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature ?: false
                    }
                    if (!isCreature) return@count false
                    val subtypes = projected?.getSubtypes(entityId)
                        ?: state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: return@count false
                    subtypes.intersect(triggeringSubtypes).isNotEmpty()
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
                StateProjector(DynamicAmountEvaluator(projectForBattlefieldCounting = false))
                    .project(state)
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
            StateProjector(DynamicAmountEvaluator(projectForBattlefieldCounting = false))
                .project(state)
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
                // Resolve from context targets
                val targetIndex = player.index
                context.targets.getOrNull(targetIndex)
                    ?.let { target ->
                        when (target) {
                            is EffectTarget.ContextTarget -> {
                                // Recursive resolution not supported, return empty
                                emptyList()
                            }
                            else -> emptyList()
                        }
                    }
                    ?: emptyList()
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
