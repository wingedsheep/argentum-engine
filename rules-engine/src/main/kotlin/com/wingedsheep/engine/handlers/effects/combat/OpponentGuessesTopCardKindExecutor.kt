package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ChooseGuessKindContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.OpponentGuessesTopCardKindEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [OpponentGuessesTopCardKindEffect] (Gollum, Scheming Guide).
 *
 * Step 1 of the opponent-guess flow: present the framing "land or nonland" choice to the
 * [OpponentGuessesTopCardKindEffect.chooser] (controller by default). The opponent's guess and the
 * reveal/compare/branch happen on resume in [com.wingedsheep.engine.handlers.continuations.GuessContinuationResumer].
 *
 * "your library" is the chooser's library — the player who owns the framing choice. We capture that
 * player as the library owner so the reveal and comparison read the correct top card.
 */
class OpponentGuessesTopCardKindExecutor : EffectExecutor<OpponentGuessesTopCardKindEffect> {

    override val effectType: KClass<OpponentGuessesTopCardKindEffect> =
        OpponentGuessesTopCardKindEffect::class

    override fun execute(
        state: GameState,
        effect: OpponentGuessesTopCardKindEffect,
        context: EffectContext
    ): EffectResult {
        // Both the framing choice and the guess may name "an opponent"; with several opponents the
        // controller picks which one, and re-running this effect resolves both to that pick.
        val chooserId = when (val outcome = ChooserResolution.resolve(state, effect.chooser, context)) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent chooses land or nonland"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "OpponentGuessesTopCardKind chooser: ${outcome.reason}")
        }
        val guesserId = when (val outcome = ChooserResolution.resolve(state, effect.guesser, context)) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent guesses"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "OpponentGuessesTopCardKind guesser: ${outcome.reason}")
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = chooserId,
            prompt = "Choose land or nonland",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = listOf("Land", "Nonland")
        )

        val continuation = ChooseGuessKindContinuation(
            decisionId = decisionId,
            controllerLibraryOwnerId = chooserId,
            guesserId = guesserId,
            onGuessedRight = effect.onGuessedRight,
            onGuessedWrong = effect.onGuessedWrong,
            effectContext = context
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = chooserId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
