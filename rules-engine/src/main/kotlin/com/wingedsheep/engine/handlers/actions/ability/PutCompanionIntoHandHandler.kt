package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CompanionPutIntoHandEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PutCompanionIntoHand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CompanionComponent
import com.wingedsheep.engine.state.components.player.companionOutsideGame
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import kotlin.reflect.KClass

/**
 * Handler for the companion special action (CR 702.139a, 116.2g): pay {3} and put your revealed
 * companion from outside the game into your hand.
 *
 * - **Timing** (CR 116.2g): any time the player has priority during a main phase of their own
 *   turn while the stack is empty — sorcery timing, though it is no spell.
 * - **Once per game** (CR 116.2g): [CompanionComponent.putIntoHand] latches.
 * - **No stack** (CR 116.1): the action happens immediately and the player keeps priority, so
 *   they may cast the companion before anyone else can act (ruling).
 *
 * Once in hand the card is an ordinary card in the game and stays in it (CR 702.139c).
 */
class PutCompanionIntoHandHandler(
    manaSolver: ManaSolver,
    manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
) : ActionHandler<PutCompanionIntoHand> {
    override val actionType: KClass<PutCompanionIntoHand> = PutCompanionIntoHand::class

    private val payment = SpecialActionManaPayment(manaSolver, manaAbilitySideEffectExecutor)

    override fun validate(state: GameState, action: PutCompanionIntoHand): String? {
        if (!state.hasPriority(action.playerId)) return "You don't have priority"
        if (!state.step.isMainPhase || state.stack.isNotEmpty() || !state.isActiveTurnFor(action.playerId)) {
            return "You can put your companion into your hand only during your main phase while the stack is empty"
        }
        val companion = state.getEntity(action.playerId)?.get<CompanionComponent>()
            ?: return "You have no companion"
        if (companion.putIntoHand) return "You've already put your companion into your hand this game"
        if (state.companionOutsideGame(action.playerId) != action.cardId) {
            return "That card isn't your companion waiting outside the game"
        }
        return payment.validate(state, action.playerId, COST, action.paymentStrategy)
    }

    override fun execute(state: GameState, action: PutCompanionIntoHand): ExecutionResult {
        val cardName = state.getEntity(action.cardId)?.get<CardComponent>()?.name
            ?: return ExecutionResult.error(state, "Companion not found")
        val events = mutableListOf<GameEvent>()

        val (paidState, paymentEvents) = payment.pay(
            state, action.playerId, COST, action.paymentStrategy, "Companion $cardName"
        ) ?: return ExecutionResult.error(state, "Not enough mana to put your companion into your hand")
        var currentState = paidState
        events.addAll(paymentEvents)

        // The companion is the acting player's own card: their sideboard to their hand.
        currentState = currentState.removeFromZone(ZoneKey(action.playerId, Zone.SIDEBOARD), action.cardId)
        val oldObject = currentState.objectRef(action.cardId)
        currentState = currentState.addToZone(ZoneKey(action.playerId, Zone.HAND), action.cardId)
        events.add(
            ZoneChangeEvent(
                entityId = action.cardId,
                entityName = cardName,
                fromZone = Zone.SIDEBOARD,
                toZone = Zone.HAND,
                ownerId = action.playerId,
                oldObject = oldObject,
                newObject = currentState.objectRef(action.cardId)
            )
        )
        currentState = currentState.updateEntity(action.playerId) { c ->
            val companion = c.get<CompanionComponent>() ?: return@updateEntity c
            c.with(companion.copy(putIntoHand = true))
        }
        events.add(CompanionPutIntoHandEvent(action.playerId, action.cardId, cardName))

        // A special action: no stack, and priority stays with the player (CR 116.1, 117.3c).
        return ExecutionResult.success(currentState.tick(), events)
    }

    companion object {
        /** The fixed cost of the companion special action (CR 702.139a). */
        val COST: ManaCost = ManaCost.parse("{3}")

        fun create(services: EngineServices): PutCompanionIntoHandHandler =
            PutCompanionIntoHandHandler(services.manaSolver, services.manaAbilitySideEffectExecutor)
    }
}
