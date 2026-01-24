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
import com.wingedsheep.sdk.scripting.DynamicAmount
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

            is DynamicAmount.LandsYouControl -> {
                countLandsControlledBy(state, context.controllerId)
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
                // TODO: Filter by land type
                countLandsControlledBy(state, opponentId) * amount.multiplier
            }

            is DynamicAmount.CreaturesOfColorTargetOpponentControls -> {
                // TODO: Count creatures of specific color
                0
            }

            is DynamicAmount.CountInZone -> {
                // TODO: Count entities matching filter in zone
                0
            }

            else -> 0
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
}
