package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CountFilter
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.PlayerReference
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.scripting.ZoneReference
import kotlin.math.max
import kotlin.math.min

/**
 * Evaluates DynamicAmount values against the current game state.
 *
 * DynamicAmount represents values that depend on game state, like
 * "the number of creatures you control" or "your life total".
 */
class DynamicAmountEvaluator {

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

            is DynamicAmount.CreaturesYouControl -> {
                countCreaturesControlledBy(state, context.controllerId)
            }

            is DynamicAmount.OtherCreaturesYouControl -> {
                val total = countCreaturesControlledBy(state, context.controllerId)
                // Subtract 1 for "other" (excluding self)
                max(0, total - 1)
            }

            is DynamicAmount.OtherCreaturesWithSubtypeYouControl -> {
                countCreaturesWithSubtype(state, context.controllerId, amount.subtype.value) -
                    if (context.sourceId != null && hasSubtype(state, context.sourceId, amount.subtype.value)) 1 else 0
            }

            is DynamicAmount.AllCreatures -> {
                state.getBattlefield().count { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
                }
            }

            is DynamicAmount.CreaturesWithSubtypeOnBattlefield -> {
                state.getBattlefield().count { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>()
                    card?.typeLine?.isCreature == true && card.typeLine.hasSubtype(amount.subtype)
                }
            }

            is DynamicAmount.LandsYouControl -> {
                countLandsControlledBy(state, context.controllerId)
            }

            is DynamicAmount.LandsWithSubtypeYouControl -> {
                countLandsOfTypeControlledBy(state, context.controllerId, amount.subtype.value)
            }

            is DynamicAmount.CardsInYourGraveyard -> {
                val graveyardZone = ZoneKey(context.controllerId, ZoneType.GRAVEYARD)
                state.getZone(graveyardZone).size
            }

            is DynamicAmount.CreatureCardsInYourGraveyard -> {
                val graveyardZone = ZoneKey(context.controllerId, ZoneType.GRAVEYARD)
                state.getZone(graveyardZone).count { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
                }
            }

            is DynamicAmount.AttackingCreaturesYouControl -> {
                state.getBattlefield().count { entityId ->
                    val container = state.getEntity(entityId) ?: return@count false
                    container.get<ControllerComponent>()?.playerId == context.controllerId &&
                        container.has<AttackingComponent>()
                }
            }

            is DynamicAmount.CreaturesAttackingYou -> {
                val attackingYou = state.getBattlefield().count { entityId ->
                    state.getEntity(entityId)?.has<AttackingComponent>() == true
                    // TODO: Check if attacking this specific player
                }
                attackingYou * amount.multiplier
            }

            is DynamicAmount.TappedCreaturesTargetOpponentControls -> {
                val opponentId = context.opponentId ?: return 0
                state.getBattlefield().count { entityId ->
                    val container = state.getEntity(entityId) ?: return@count false
                    container.get<ControllerComponent>()?.playerId == opponentId &&
                        container.get<CardComponent>()?.typeLine?.isCreature == true &&
                        container.has<TappedComponent>()
                }
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

            is DynamicAmount.CreaturesEnteredThisTurn -> {
                // TODO: Track ETB this turn
                0
            }

            is DynamicAmount.HandSizeDifferenceFromTargetOpponent -> {
                val myHand = state.getZone(ZoneKey(context.controllerId, ZoneType.HAND)).size
                val opponentHand = context.opponentId?.let {
                    state.getZone(ZoneKey(it, ZoneType.HAND)).size
                } ?: 0
                max(0, opponentHand - myHand)
            }

            is DynamicAmount.LandsOfTypeTargetOpponentControls -> {
                val opponentId = context.opponentId ?: return 0
                countLandsOfTypeControlledBy(state, opponentId, amount.landType) * amount.multiplier
            }

            is DynamicAmount.CreaturesOfColorTargetOpponentControls -> {
                val opponentId = context.opponentId ?: return 0
                countCreaturesOfColorControlledBy(state, opponentId, amount.color) * amount.multiplier
            }

            is DynamicAmount.CountInZone -> {
                evaluateCountInZone(state, amount, context)
            }

            is DynamicAmount.CountPermanents -> {
                // Convenience wrapper for CountInZone with battlefield
                val countInZone = DynamicAmount.CountInZone(
                    player = amount.controller,
                    zone = ZoneReference.Battlefield,
                    filter = amount.filter
                )
                evaluateCountInZone(state, countInZone, context)
            }

            // Unified counting (new filter architecture)
            is DynamicAmount.Count -> {
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context)
            }

            is DynamicAmount.CountBattlefield -> {
                evaluateUnifiedCount(state, amount.player, Zone.Battlefield, amount.filter, context)
            }
        }
    }

    private fun countCreaturesControlledBy(state: GameState, playerId: EntityId): Int {
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.isCreature == true
        }
    }

    private fun countLandsControlledBy(state: GameState, playerId: EntityId): Int {
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.isLand == true
        }
    }

    private fun countLandsOfTypeControlledBy(state: GameState, playerId: EntityId, landType: String): Int {
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.let { typeLine ->
                    typeLine.isLand && typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(landType))
                } == true
        }
    }

    private fun countCreaturesOfColorControlledBy(
        state: GameState,
        playerId: EntityId,
        color: com.wingedsheep.sdk.core.Color
    ): Int {
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.let { card ->
                    card.typeLine.isCreature && color in card.colors
                } == true
        }
    }

    private fun countCreaturesWithSubtype(state: GameState, playerId: EntityId, subtype: String): Int {
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.hasSubtype(
                    com.wingedsheep.sdk.core.Subtype(subtype)
                ) == true
        }
    }

    private fun hasSubtype(state: GameState, entityId: EntityId, subtype: String): Boolean {
        return state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.hasSubtype(
            com.wingedsheep.sdk.core.Subtype(subtype)
        ) == true
    }

    private fun evaluateCountInZone(
        state: GameState,
        amount: DynamicAmount.CountInZone,
        context: EffectContext
    ): Int {
        val playerIds = resolvePlayerIds(state, amount.player, context)
        val zoneType = resolveZoneType(amount.zone)

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == ZoneType.BATTLEFIELD) {
                // Battlefield is shared, filter by controller
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId == playerId
                }
            } else {
                state.getZone(ZoneKey(playerId, zoneType))
            }

            entities.count { entityId ->
                matchesCountFilter(state, entityId, amount.filter)
            }
        }
    }

    private fun resolvePlayerIds(
        state: GameState,
        player: PlayerReference,
        context: EffectContext
    ): List<EntityId> {
        return when (player) {
            is PlayerReference.You -> listOf(context.controllerId)
            is PlayerReference.Opponent -> {
                state.turnOrder.filter { it != context.controllerId }
            }
            is PlayerReference.TargetOpponent -> {
                listOfNotNull(context.opponentId)
            }
            is PlayerReference.TargetPlayer -> {
                // TargetPlayer uses the targeted player from context
                // Falls back to opponentId if no specific target
                listOfNotNull(context.opponentId)
            }
            is PlayerReference.Each -> {
                state.turnOrder
            }
        }
    }

    private fun resolveZoneType(zone: ZoneReference): ZoneType {
        return when (zone) {
            is ZoneReference.Hand -> ZoneType.HAND
            is ZoneReference.Battlefield -> ZoneType.BATTLEFIELD
            is ZoneReference.Graveyard -> ZoneType.GRAVEYARD
            is ZoneReference.Library -> ZoneType.LIBRARY
            is ZoneReference.Exile -> ZoneType.EXILE
        }
    }

    // =========================================================================
    // Unified Filter Evaluation (new architecture)
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
        val zoneType = resolveUnifiedZoneType(zone)

        val predicateContext = PredicateContext.fromEffectContext(context)

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == ZoneType.BATTLEFIELD) {
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
        }
    }

    private fun resolveUnifiedZoneType(zone: Zone): ZoneType {
        return when (zone) {
            Zone.Hand -> ZoneType.HAND
            Zone.Battlefield -> ZoneType.BATTLEFIELD
            Zone.Graveyard -> ZoneType.GRAVEYARD
            Zone.Library -> ZoneType.LIBRARY
            Zone.Exile -> ZoneType.EXILE
            Zone.Stack -> ZoneType.STACK
            Zone.Command -> ZoneType.COMMAND
        }
    }

    // =========================================================================
    // Legacy Filter Evaluation (deprecated filters)
    // =========================================================================

    private fun matchesCountFilter(state: GameState, entityId: EntityId, filter: CountFilter): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val card = container.get<CardComponent>() ?: return false

        return when (filter) {
            is CountFilter.Any -> true

            is CountFilter.Creatures -> card.typeLine.isCreature

            is CountFilter.TappedCreatures -> {
                card.typeLine.isCreature && container.has<TappedComponent>()
            }

            is CountFilter.UntappedCreatures -> {
                card.typeLine.isCreature && !container.has<TappedComponent>()
            }

            is CountFilter.Lands -> card.typeLine.isLand

            is CountFilter.LandType -> {
                card.typeLine.isLand &&
                    card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.landType))
            }

            is CountFilter.CreatureColor -> {
                card.typeLine.isCreature && filter.color in card.colors
            }

            is CountFilter.CardColor -> {
                filter.color in card.colors
            }

            is CountFilter.HasSubtype -> {
                card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
            }

            is CountFilter.AttackingCreatures -> {
                card.typeLine.isCreature && container.has<AttackingComponent>()
            }

            is CountFilter.And -> {
                filter.filters.all { matchesCountFilter(state, entityId, it) }
            }

            is CountFilter.Or -> {
                filter.filters.any { matchesCountFilter(state, entityId, it) }
            }
        }
    }
}
