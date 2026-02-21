package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.EachPlayerChoosesCreatureTypeEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerChoosesCreatureTypeEffect.
 *
 * Each player (in APNAP order) chooses a creature type. After all players have chosen,
 * the accumulated chosen types are stored in storedStringLists[storeAs] on the
 * EffectContinuation below on the stack.
 */
class EachPlayerChoosesCreatureTypeExecutor : EffectExecutor<EachPlayerChoosesCreatureTypeEffect> {

    override val effectType: KClass<EachPlayerChoosesCreatureTypeEffect> = EachPlayerChoosesCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerChoosesCreatureTypeEffect,
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

        val continuation = EachPlayerChoosesCreatureTypeContinuation(
            decisionId = decisionId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = firstPlayer,
            remainingPlayers = remainingPlayers,
            chosenTypes = emptyList(),
            creatureTypes = allCreatureTypes,
            storeAs = effect.storeAs
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
