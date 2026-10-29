package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.VoteEffect
import kotlin.reflect.KClass

/**
 * Executor for [VoteEffect] (CR 701.38): starting with the controller and proceeding in turn order,
 * each player still in the game votes for one of the choices. Every vote pauses for a
 * [ChooseOptionDecision] whose prompt shows the votes cast so far, and is announced with a
 * [VoteCastEvent]; [com.wingedsheep.engine.handlers.continuations.VoteContinuationResumer] asks the
 * next voter and, after the last vote, publishes each tally under its pipeline number name.
 */
class VoteExecutor : EffectExecutor<VoteEffect> {

    override val effectType: KClass<VoteEffect> = VoteEffect::class

    override fun execute(state: GameState, effect: VoteEffect, context: EffectContext): EffectResult {
        val voters = state.turnOrderStartingWith(context.controllerId)
        val first = voters.firstOrNull()
            ?: return EffectResult(state = state, updatedStoredNumbers = effect.voteCounts.mapValues { 0 })
        val continuation = VoteContinuation(
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            currentPlayerId = first,
            remainingVoters = voters.drop(1),
            choices = effect.choices,
            tallies = effect.choices.associateWith { 0 },
            voteCounts = effect.voteCounts,
            objectReferences = context.objectReferences,
        )
        return EffectResult.from(askVote(state, continuation))
    }

    companion object {
        /** Pause for [continuation]'s current voter to vote. */
        fun askVote(
            state: GameState,
            continuation: VoteContinuation,
            events: List<GameEvent> = emptyList(),
        ): ExecutionResult = state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = continuation.currentPlayerId,
                    prompt = votePrompt(continuation),
                    context = DecisionContext(
                        sourceId = continuation.sourceId,
                        sourceName = continuation.sourceName,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = continuation.choices.map { choice -> choice.replaceFirstChar { it.uppercase() } }
                )
            },
            answer = continuation,
            events = events,
        )

        private fun votePrompt(continuation: VoteContinuation): String {
            val choices = continuation.choices
            val ask = "Vote for " + when (choices.size) {
                2 -> "${choices[0]} or ${choices[1]}"
                else -> choices.dropLast(1).joinToString(", ") + ", or " + choices.last()
            }
            if (continuation.tallies.values.all { it == 0 }) return ask
            val soFar = choices.joinToString(", ") { "$it ${continuation.tallies[it] ?: 0}" }
            return "$ask (votes so far: $soFar)"
        }
    }
}
