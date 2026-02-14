package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.HarshMercyEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for HarshMercyEffect.
 *
 * "Each player chooses a creature type. Destroy all creatures that aren't of a type
 * chosen this way. They can't be regenerated."
 *
 * This executor:
 * 1. Presents a ChooseOptionDecision to the active player (APNAP order)
 * 2. Pushes a HarshMercyContinuation to track choices
 * 3. On resume, records choice, asks next player or executes destruction
 */
class HarshMercyExecutor : EffectExecutor<HarshMercyEffect> {

    override val effectType: KClass<HarshMercyEffect> = HarshMercyEffect::class

    override fun execute(
        state: GameState,
        effect: HarshMercyEffect,
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

        val continuation = HarshMercyContinuation(
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
