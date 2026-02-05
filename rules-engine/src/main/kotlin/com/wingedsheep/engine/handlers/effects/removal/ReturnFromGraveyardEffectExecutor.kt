package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import kotlin.reflect.KClass

/**
 * Executor for ReturnFromGraveyardEffect.
 * "Return target creature card from your graveyard to your hand"
 * "Return target creature card from a graveyard to the battlefield under your control"
 *
 * Handles returning cards from graveyard to:
 * - Hand (Gravedigger, Raise Dead)
 * - Battlefield (Breath of Life, Reanimate)
 *
 * Uses the standard targeting flow: the card declares a TargetCardInGraveyard requirement,
 * and the chosen target is available in context.targets.
 */
class ReturnFromGraveyardEffectExecutor : EffectExecutor<ReturnFromGraveyardEffect> {

    override val effectType: KClass<ReturnFromGraveyardEffect> = ReturnFromGraveyardEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnFromGraveyardEffect,
        context: EffectContext
    ): ExecutionResult {
        val target = context.targets.firstOrNull()
            ?: return ExecutionResult.error(state, "No target for return from graveyard effect")

        val (cardId, ownerId) = when (target) {
            is ChosenTarget.Card -> target.cardId to target.ownerId
            is ChosenTarget.Permanent -> target.entityId to context.controllerId
            else -> return ExecutionResult.error(state, "Invalid target type for return from graveyard")
        }

        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) {
            return ExecutionResult.error(state, "Card not in graveyard")
        }

        return when (effect.destination) {
            SearchDestination.HAND -> moveToHand(state, cardId, cardComponent, ownerId, context.controllerId)
            SearchDestination.BATTLEFIELD -> moveToBattlefield(state, cardId, cardComponent, ownerId, context.controllerId)
            else -> ExecutionResult.error(state, "Unsupported destination: ${effect.destination}")
        }
    }

    private fun moveToHand(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId
    ): ExecutionResult {
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        val handZone = ZoneKey(ownerId, ZoneType.HAND)

        var newState = state.removeFromZone(graveyardZone, cardId)
        newState = newState.addToZone(handZone, cardId)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    cardId,
                    cardComponent.name,
                    ZoneType.GRAVEYARD,
                    ZoneType.HAND,
                    ownerId
                )
            )
        )
    }

    private fun moveToBattlefield(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId
    ): ExecutionResult {
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)

        var newState = state.removeFromZone(graveyardZone, cardId)
        newState = newState.addToZone(battlefieldZone, cardId)

        newState = newState.updateEntity(cardId) { c ->
            c.with(ControllerComponent(controllerId))
                .with(SummoningSicknessComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    cardId,
                    cardComponent.name,
                    ZoneType.GRAVEYARD,
                    ZoneType.BATTLEFIELD,
                    controllerId
                )
            )
        )
    }
}
