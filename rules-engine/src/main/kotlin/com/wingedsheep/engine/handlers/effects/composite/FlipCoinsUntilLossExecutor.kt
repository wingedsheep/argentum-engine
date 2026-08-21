package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.FlipCoinsUntilLossContinuation
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.CoinFlipModifiers
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
 * One call flips exactly **one** coin, so the executor can never spin: a lost flip finishes the run
 * synchronously, and a won flip pauses on "flip again?". Every later flip arrives through
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
 * Unlike [FlipCoinsExecutor], [CoinFlipModifiers.shouldForceWin] is consulted **per flip** and
 * [CoinFlipModifiers.markFlipped] runs after each one. `FlipCoinsEffect` flips its whole batch as a
 * single "flip N coins" event, so one replacement decision covers all of it; here each coin is its own
 * flip, so a "the first time you flip one or more coins each turn" replacement (Edgar, King of Figaro)
 * applies to the first coin only and the rest are honest flips.
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
        flipperId = context.controllerId,
        storeWinsAs = effect.storeWinsAs,
        winsSoFar = 0,
        sourceId = context.sourceId,
        cardRegistry = cardRegistry,
        decisionHandler = decisionHandler,
        priorEvents = emptyList()
    )

    companion object {

        /**
         * Flip one coin for [flipperId] on top of [winsSoFar] already-won flips.
         *
         * Returns either a finished result carrying the final tally under [storeWinsAs], or a pause on
         * the "flip again?" question with a [FlipCoinsUntilLossContinuation] holding the new tally.
         */
        fun flipOnce(
            state: GameState,
            flipperId: EntityId,
            storeWinsAs: String,
            winsSoFar: Int,
            sourceId: EntityId?,
            cardRegistry: CardRegistry,
            decisionHandler: DecisionHandler,
            priorEvents: List<GameEvent>
        ): EffectResult {
            val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

            val forced = CoinFlipModifiers.shouldForceWin(state, cardRegistry, flipperId)
            val (won, afterFlip) = if (forced) true to state else state.nextRandom { nextBoolean() }
            val afterMark = CoinFlipModifiers.markFlipped(afterFlip, flipperId)

            val events = priorEvents +
                CoinFlipEvent(flipperId, won, sourceId ?: flipperId, sourceName)

            if (!won) {
                // The run ends on a lost flip and the flip that lost is not counted. Losing the first
                // flip therefore stores 0 — which is how "if you lose a flip, this has no effect" is
                // modelled: every `GTE 1` payoff gate reads the zero and falls away on its own.
                return finish(afterMark, storeWinsAs, winsSoFar, events)
            }

            val wins = winsSoFar + 1

            // Backstop, not a game rule: the loop is only bounded by the flipper answering "stop", and
            // a forced-win replacement plus an always-yes automated answer would never produce one.
            // Far above any real run — the card that motivates this pays out fully at three.
            if (wins >= GameLimits.MAX_COIN_FLIPS_PER_EFFECT) {
                System.err.println(
                    "GameLimits: flip-until-loss reached $wins won flips — stopping " +
                        "(likely a forced-win replacement with an automated 'continue' answer)."
                )
                return finish(afterMark, storeWinsAs, wins, events)
            }

            val decisionResult = decisionHandler.createYesNoDecision(
                state = afterMark,
                playerId = flipperId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Flip another coin? You have won $wins " +
                    if (wins == 1) "flip so far." else "flips so far.",
                yesText = "Flip again",
                noText = "Stop flipping",
                phase = DecisionPhase.RESOLUTION
            )
            val decision = decisionResult.pendingDecision
                ?: return EffectResult.error(afterMark, "Failed to create continue-flipping decision")

            val continuation = FlipCoinsUntilLossContinuation(
                decisionId = decision.id,
                flipperId = flipperId,
                storeWinsAs = storeWinsAs,
                winsSoFar = wins,
                sourceId = sourceId
            )

            return EffectResult.paused(
                decisionResult.state.pushContinuation(continuation),
                decision,
                events + decisionResult.events
            )
        }

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
