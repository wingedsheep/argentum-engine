package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.PatriarchsBiddingEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PatriarchsBiddingEffect.
 *
 * "Each player chooses a creature type. Each player returns all creature cards of a type
 * chosen this way from their graveyard to the battlefield."
 *
 * This executor:
 * 1. Presents a ChooseOptionDecision to the active player (APNAP order)
 * 2. Pushes a PatriarchsBiddingContinuation to track choices
 * 3. On resume, records choice, asks next player or executes mass reanimation
 */
class PatriarchsBiddingExecutor : EffectExecutor<PatriarchsBiddingEffect> {

    override val effectType: KClass<PatriarchsBiddingEffect> = PatriarchsBiddingEffect::class

    override fun execute(
        state: GameState,
        effect: PatriarchsBiddingEffect,
        context: EffectContext
    ): ExecutionResult {
        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Get players in APNAP order (active player first)
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")
        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        val firstPlayer = playerOrder.first()
        val remainingPlayers = playerOrder.drop(1)

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = firstPlayer,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes
        )

        val continuation = PatriarchsBiddingContinuation(
            decisionId = decisionId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = firstPlayer,
            remainingPlayers = remainingPlayers,
            chosenTypes = emptyList(),
            creatureTypes = allCreatureTypes
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = firstPlayer,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
