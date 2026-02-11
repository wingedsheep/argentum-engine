package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Evaluates DynamicAmount values against the current game state.
 *
 * DynamicAmount represents values that depend on game state, like
 * "the number of creatures you control" or "your life total".
 */
class DynamicAmountEvaluator(
    private val conditionEvaluator: ConditionEvaluator? = null
) {

    /**
     * Evaluate a DynamicAmount to get an actual integer value.
     */
    fun evaluate(
        state: GameState,
        amount: DynamicAmount,
        context: EffectContext
    ): Int {
        return when (amount) {
            is DynamicAmount.Fixed -> amount.amount

            is DynamicAmount.XValue -> context.xValue ?: 0

            is DynamicAmount.YourLifeTotal -> {
                state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: 0
            }

            is DynamicAmount.VariableReference -> {
                // TODO: Implement variable storage
                0
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
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context)
            }

            is DynamicAmount.CountBattlefield -> {
                evaluateUnifiedCount(state, amount.player, Zone.BATTLEFIELD, amount.filter, context)
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

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context) else evaluate(state, amount.ifFalse, context)
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
        context: EffectContext
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, player, context)
        val zoneType = resolveUnifiedZone(zone)

        val predicateContext = PredicateContext.fromEffectContext(context)

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == Zone.BATTLEFIELD) {
                // Battlefield is shared, filter by controller
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId == playerId
                }
            } else {
                state.getZone(ZoneKey(playerId, zoneType))
            }

            entities.count { entityId ->
                predicateEvaluator.matches(state, entityId, filter, predicateContext)
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
                            is com.wingedsheep.sdk.scripting.EffectTarget.ContextTarget -> {
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
        }
    }

    private fun resolveUnifiedZone(zone: Zone): Zone = zone
}
