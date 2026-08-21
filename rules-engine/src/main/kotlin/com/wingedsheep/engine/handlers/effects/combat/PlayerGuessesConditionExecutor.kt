package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GuessConditionContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.PlayerGuessesConditionEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [PlayerGuessesConditionEffect] — "[a player] guesses whether [condition] is true"
 * (Liar's Pendulum).
 *
 * All this half does is put the question. The condition is deliberately **not** evaluated here: the
 * guesser must not be able to learn the answer from anything this effect does, and the whole frame is
 * carried to
 * [com.wingedsheep.engine.handlers.continuations.GuessContinuationResumer] where the answer is scored
 * once it is in.
 *
 * The question is a plain yes/no rather than a two-option list because a guess *is* a yes/no about one
 * proposition, and the yes/no UI already reads as an answer rather than a selection.
 */
class PlayerGuessesConditionExecutor : EffectExecutor<PlayerGuessesConditionEffect> {

    override val effectType: KClass<PlayerGuessesConditionEffect> = PlayerGuessesConditionEffect::class

    override fun execute(
        state: GameState,
        effect: PlayerGuessesConditionEffect,
        context: EffectContext
    ): EffectResult {
        val guesserId = when (val outcome = ChooserResolution.resolve(state, effect.guesser, context)) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent guesses"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "PlayerGuessesCondition guesser: ${outcome.reason}")
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // `{name}` lets the question name the card the guess is about. The name lives in chosenValues
        // because an earlier step chose it, so it can only be substituted here, not at authoring time.
        val prompt = effect.promptNameVariable
            ?.let { context.pipeline.chosenValues[it] }
            ?.let { effect.prompt.replace("{name}", it) }
            ?: effect.prompt

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = guesserId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Yes",
            noText = "No"
        )

        val continuation = GuessConditionContinuation(
            decisionId = decisionId,
            guesserId = guesserId,
            condition = effect.condition,
            storeGuessedRightAs = effect.storeGuessedRightAs,
            effectContext = context
        )

        return EffectResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = guesserId,
                    decisionType = "YES_NO",
                    prompt = prompt
                )
            )
        )
    }
}
