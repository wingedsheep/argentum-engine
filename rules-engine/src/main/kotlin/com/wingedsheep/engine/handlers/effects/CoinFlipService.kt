package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The single place a coin is actually flipped.
 *
 * Every coin-flip executor asks this service for its coins rather than calling
 * [GameState.nextRandom] itself, so both coin-flip replacements apply uniformly wherever a flip
 * happens: [com.wingedsheep.sdk.scripting.WinCoinFlips] (all coins come up heads, CR 705.3) and
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] (each coin becomes several, all but one
 * ignored — Krark's Thumb).
 *
 * "Heads" is a won flip throughout the coin plumbing, so a forced win is a forced heads.
 *
 * ### Why this is a two-phase API
 *
 * "Flip two coins and ignore one" is a *choice*, so a flip can pause mid-resolution. The service
 * therefore returns either a [Resolution.Resolved] with one result per coin the caller asked for,
 * or a [Resolution.NeedsChoice] carrying the decision plus the [PendingCoinFlipChoice] the caller
 * must park in a [com.wingedsheep.engine.core.CoinFlipChoiceContinuation]. Resuming calls
 * [advanceAfterAnswer], which either raises the next question or finishes the batch — so a caller
 * never has to know how many prompts a flip will cost.
 *
 * ### What the rulings pin down
 *
 * - The replacement applies to **each individual coin**, not to the instruction: "flip five coins"
 *   under one Krark's Thumb is five *pairs*, one kept from each — not ten coins with five ignored.
 *   That is why [flip] takes a `count` and builds one batch per coin.
 * - **Every coin is flipped before any is ignored** ("You will know the results of all simultaneous
 *   flips before choosing which to ignore"), so [flip] rolls the whole batch up front and only then
 *   starts asking.
 * - A batch whose coins all agree offers nothing to choose, so no question is raised for it. That
 *   also means a [com.wingedsheep.sdk.scripting.WinCoinFlips] replacement — which makes every coin
 *   heads — silently costs no prompts.
 */
object CoinFlipService {

    /**
     * A coin-flip batch part-way through being resolved: the raw coins that were flipped for each
     * coin the caller asked for, plus the kept result of every batch decided so far.
     *
     * [decided] fills [batches] from the front, so `decided.size` is both the number of finished
     * batches and the index of the one being asked about. Rides in
     * [com.wingedsheep.engine.core.CoinFlipChoiceContinuation] across the pause.
     */
    @Serializable
    data class PendingCoinFlipChoice(
        val batches: List<List<Boolean>>,
        val decided: List<Boolean> = emptyList(),
        val flipperId: EntityId,
        val sourceId: EntityId? = null,
        val sourceName: String = "Unknown"
    )

    /** Either a finished batch of flips, or a question the flipper still owes an answer to. */
    sealed interface Resolution {

        /**
         * The batch is done. [results] has one entry per coin the caller asked for — `true` is
         * heads / a won flip — and [events] reports every coin that was really flipped, with the
         * discarded ones carrying [CoinFlipEvent.ignored].
         */
        data class Resolved(
            val state: GameState,
            val results: List<Boolean>,
            val events: List<GameEvent>
        ) : Resolution

        /**
         * The flipper must say which coin to keep. [events] carries the flips already settled (so
         * they reach the log at the pause); [pending] must be stored in the caller's continuation
         * and handed back to [advanceAfterAnswer] with the answer.
         */
        data class NeedsChoice(
            val state: GameState,
            val decision: PendingDecision,
            val pending: PendingCoinFlipChoice,
            val events: List<GameEvent>
        ) : Resolution
    }

    /**
     * Flip [count] coins for [flipperId], applying every coin-flip replacement they control.
     *
     * [count] is the number of coins the *game* asked for; a replacement may turn each into several
     * real flips. A [count] of zero is a no-op and does not even mark the player as having flipped.
     */
    fun flip(
        state: GameState,
        flipperId: EntityId,
        count: Int,
        sourceId: EntityId?,
        cardRegistry: CardRegistry,
        decisionHandler: DecisionHandler
    ): Resolution {
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val wanted = count.coerceAtLeast(0)
        if (wanted == 0) return Resolution.Resolved(state, emptyList(), emptyList())

        val forced = CoinFlipModifiers.shouldForceWin(state, cardRegistry, flipperId)
        val coinsPerFlip = CoinFlipModifiers.coinsPerFlip(state, cardRegistry, flipperId)

        var current = state
        val batches = List(wanted) {
            List(coinsPerFlip) {
                if (forced) {
                    true
                } else {
                    val (result, advanced) = current.nextRandom { nextBoolean() }
                    current = advanced
                    result
                }
            }
        }

        // One "you flipped coins" mark per flip event, as before — the replacement multiplies the
        // coins inside the event, it does not turn one event into several.
        current = CoinFlipModifiers.markFlipped(current, flipperId)

        return advance(
            current,
            PendingCoinFlipChoice(batches, emptyList(), flipperId, sourceId, sourceName),
            emptyList(),
            decisionHandler
        )
    }

    /**
     * Resume a paused batch: record [keepHeads] as the kept result of the batch that was being
     * asked about, then carry on to the next undecided batch (or finish).
     */
    fun advanceAfterAnswer(
        state: GameState,
        pending: PendingCoinFlipChoice,
        keepHeads: Boolean,
        decisionHandler: DecisionHandler
    ): Resolution {
        val index = pending.decided.size
        if (index >= pending.batches.size) {
            return Resolution.Resolved(state, pending.decided, emptyList())
        }
        val settled = pending.copy(decided = pending.decided + keepHeads)
        return advance(state, settled, eventsForBatch(settled, index), decisionHandler)
    }

    /**
     * Settle every batch that needs no input, stopping at the first that does.
     *
     * [eventsSoFar] are flips already settled on this pass and not yet published; they ride out on
     * whichever [Resolution] this returns.
     */
    private fun advance(
        state: GameState,
        pending: PendingCoinFlipChoice,
        eventsSoFar: List<GameEvent>,
        decisionHandler: DecisionHandler
    ): Resolution {
        var settled = pending
        val events = eventsSoFar.toMutableList()

        while (settled.decided.size < settled.batches.size) {
            val index = settled.decided.size
            val batch = settled.batches[index]

            // Nothing to choose when the coins agree — including the trivial one-coin batch of a
            // player with no "flip additional coins" replacement, and any batch a forced-win
            // replacement made unanimously heads.
            val unanimous = batch.all { it } || batch.none { it }
            if (unanimous) {
                settled = settled.copy(decided = settled.decided + batch.first())
                events += eventsForBatch(settled, index)
                continue
            }

            val decisionResult = decisionHandler.createYesNoDecision(
                state = state,
                playerId = settled.flipperId,
                sourceId = settled.sourceId,
                sourceName = settled.sourceName,
                prompt = choicePrompt(settled, index),
                yesText = "Keep heads",
                noText = "Keep tails",
                phase = DecisionPhase.RESOLUTION
            )
            val decision = decisionResult.pendingDecision
                ?: return Resolution.Resolved(
                    // Fail closed on the honest result rather than inventing a choice: without a
                    // decision the flipper cannot pick, so the batch keeps its first coin.
                    state,
                    settled.decided + batch.first(),
                    events + eventsForBatch(settled.copy(decided = settled.decided + batch.first()), index)
                )

            return Resolution.NeedsChoice(
                decisionResult.state,
                decision,
                settled,
                events + decisionResult.events
            )
        }

        return Resolution.Resolved(state, settled.decided, events)
    }

    /**
     * The flips of batch [index], now that its kept result is known: exactly one coin matching the
     * kept result counts, and every other coin in the batch is reported as ignored.
     */
    private fun eventsForBatch(pending: PendingCoinFlipChoice, index: Int): List<GameEvent> {
        val batch = pending.batches[index]
        val kept = pending.decided[index]
        val keptPosition = batch.indexOf(kept)
        return batch.mapIndexed { position, coin ->
            CoinFlipEvent(
                playerId = pending.flipperId,
                won = coin,
                sourceId = pending.sourceId ?: pending.flipperId,
                sourceName = pending.sourceName,
                ignored = position != keptPosition
            )
        }
    }

    private fun choicePrompt(pending: PendingCoinFlipChoice, index: Int): String {
        val batch = pending.batches[index]
        val heads = batch.count { it }
        val tails = batch.size - heads
        val rolled = "You flipped $heads heads and $tails tails"
        val which =
            if (pending.batches.size == 1) "."
            else " for flip ${index + 1} of ${pending.batches.size}."
        return "$rolled$which Ignore all but one — which result do you keep?"
    }
}
