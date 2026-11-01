package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CoinFlipChoiceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.FlipCoinsUntilLossContinuation
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.CoinFlipService
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FlipCoinsUntilLossEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipCoinsUntilLossEffect] — "flip a coin until you lose a flip or choose to stop
 * flipping", tallying the flips won (Fiery Gambit).
 *
 * In the optional form (`mayStop = true`) one call flips exactly **one** coin: a lost flip finishes
 * the run synchronously, and a won flip pauses on "flip again?". The mandatory form
 * (`mayStop = false`, Mirror March) has no question to ask, so a won flip is followed by the next
 * flip in the same call, bounded by [GameLimits.MAX_COIN_FLIPS_PER_EFFECT]. Every later optional flip arrives through
 * [FlipCoinsUntilLossContinuation] on the resume path, and the running tally rides *in that frame* —
 * not in the pipeline. That is deliberate: pipeline `storedNumbers` only reach a consumer when they
 * are published on the finishing result, so a tally threaded through the pipeline would be lost on
 * each pause and the card would behave differently depending on whether a prompt happened to be
 * raised. The tally is therefore published exactly once, when the run ends.
 *
 * Flip → check → ask is the order the card's own ruling demands ("after each flip, you choose whether
 * to continue flipping"): the choice only ever follows a *won* flip, because a lost flip has already
 * ended the run and there is nothing left to decide.
 *
 * This run has **two** pause points, and they must not be confused. The coin itself can pause under a
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb: "which of these two
 * do you keep?", a [CoinFlipChoiceContinuation]), and only once that coin has settled does the
 * "flip again?" question arise. The tally therefore has to survive the *first* pause too, which is
 * why [CoinFlipChoiceContinuation.winsSoFar] exists.
 *
 * Unlike [FlipCoinsExecutor], each coin here is its own flip event: [CoinFlipService] is called once
 * per coin, so a "the first time you flip one or more coins each turn" replacement (Edgar, King of
 * Figaro) applies to the first coin only and the rest are honest flips.
 */
class FlipCoinsUntilLossExecutor(
    private val cardRegistry: CardRegistry,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<FlipCoinsUntilLossEffect> {

    override val effectType: KClass<FlipCoinsUntilLossEffect> = FlipCoinsUntilLossEffect::class

    override fun execute(
        state: GameState,
        effect: FlipCoinsUntilLossEffect,
        context: EffectContext
    ): EffectResult = flipOnce(
        state = state,
        effect = effect,
        context = context,
        winsSoFar = 0,
        cardRegistry = cardRegistry,
        decisionHandler = decisionHandler,
        priorEvents = emptyList()
    )

    companion object {

        /**
         * Flip one coin for the run's flipper on top of [winsSoFar] already-won flips.
         *
         * Returns a finished result carrying the final tally under
         * [FlipCoinsUntilLossEffect.storeWinsAs], a pause on "which coin do you keep?" when a
         * flip-additional-coins replacement applies, or a pause on "flip again?" after a win.
         */
        fun flipOnce(
            state: GameState,
            effect: FlipCoinsUntilLossEffect,
            context: EffectContext,
            winsSoFar: Int,
            cardRegistry: CardRegistry,
            decisionHandler: DecisionHandler,
            priorEvents: List<GameEvent>
        ): EffectResult {
            val resolution = CoinFlipService.flip(
                state = state,
                flipperId = context.controllerId,
                count = 1,
                sourceId = context.sourceId,
                cardRegistry = cardRegistry,
            )

            return when (resolution) {
                is CoinFlipService.Resolution.NeedsChoice -> EffectResult.from(resolution.state.suspendForDecision(resolution.question, CoinFlipChoiceContinuation(
                            effect = effect,
                            effectContext = context,
                            pending = resolution.pending,
                            winsSoFar = winsSoFar
                        ), priorEvents + resolution.events))

                is CoinFlipService.Resolution.Resolved -> afterFlip(
                    state = resolution.state,
                    effect = effect,
                    context = context,
                    won = resolution.results.firstOrNull() == true,
                    winsSoFar = winsSoFar,
                    cardRegistry = cardRegistry,
                    decisionHandler = decisionHandler,
                    priorEvents = priorEvents + resolution.events
                )
            }
        }

        /**
         * Continue the run now that one coin has settled: a lost flip ends it, a won flip asks
         * whether to keep going (or, when the effect's `mayStop` is false, flips again). Shared with the resume path so a coin that paused for a Krark's
         * Thumb choice rejoins the run exactly where an unreplaced coin would have.
         */
        fun afterFlip(
            state: GameState,
            effect: FlipCoinsUntilLossEffect,
            context: EffectContext,
            won: Boolean,
            winsSoFar: Int,
            cardRegistry: CardRegistry,
            decisionHandler: DecisionHandler,
            priorEvents: List<GameEvent>
        ): EffectResult {
            if (!won) {
                // The run ends on a lost flip and the flip that lost is not counted. Losing the first
                // flip therefore stores 0 — which is how "if you lose a flip, this has no effect" is
                // modelled: every `GTE 1` payoff gate reads the zero and falls away on its own.
                return finish(state, effect.storeWinsAs, winsSoFar, priorEvents)
            }

            val wins = winsSoFar + 1

            // Backstop, not a game rule: under a forced-win replacement no flip can be lost, so the run
            // is bounded only by the flipper answering "stop" — which an always-yes automated answer
            // never gives, and the mandatory form (mayStop = false) never offers.
            // Far above any real run — the card that motivates this pays out fully at three.
            if (wins >= GameLimits.MAX_COIN_FLIPS_PER_EFFECT) {
                System.err.println(
                    "GameLimits: flip-until-loss reached $wins won flips — stopping " +
                        "(likely a forced-win replacement with a mandatory run or an automated 'continue' answer)."
                )
                return finish(state, effect.storeWinsAs, wins, priorEvents)
            }

            // The mandatory form ("flip a coin until you lose a flip") has no choice to offer: a won
            // flip is simply followed by the next one. Each coin still goes through flipOnce, so a
            // Krark's Thumb pause mid-run keeps this effect (and its mayStop) in its frame.
            if (!effect.mayStop) {
                return flipOnce(
                    state = state,
                    effect = effect,
                    context = context,
                    winsSoFar = wins,
                    cardRegistry = cardRegistry,
                    decisionHandler = decisionHandler,
                    priorEvents = priorEvents
                )
            }

            val sourceId = context.sourceId
            val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

            val continuation = FlipCoinsUntilLossContinuation(
                flipperId = context.controllerId,
                storeWinsAs = effect.storeWinsAs,
                winsSoFar = wins,
                sourceId = sourceId
            )

            val decisionResult = decisionHandler.createYesNoDecision(
                state = state,
                playerId = context.controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Flip another coin? You have won $wins " +
                    if (wins == 1) "flip so far." else "flips so far.",
                yesText = "Flip again",
                noText = "Stop flipping",
                phase = DecisionPhase.RESOLUTION,
                answer = continuation
            )
            val decision = decisionResult.pendingDecision
                ?: return EffectResult.error(state, "Failed to create continue-flipping decision")

            return EffectResult.propagatePause(
                decisionResult.state,
                priorEvents + decisionResult.events
            )
        }

        /** The run's flipper and source, rebuilt for a resume that only kept the frame's fields. */
        fun contextFor(flipperId: EntityId, sourceId: EntityId?): EffectContext =
            EffectContext(sourceId = sourceId, controllerId = flipperId)

        private fun finish(
            state: GameState,
            storeWinsAs: String,
            wins: Int,
            events: List<GameEvent>
        ): EffectResult = EffectResult(
            state = state,
            events = events,
            updatedStoredNumbers = mapOf(storeWinsAs to wins)
        )
    }
}
