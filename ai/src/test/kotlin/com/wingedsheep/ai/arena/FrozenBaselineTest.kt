package com.wingedsheep.ai.arena

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Guards `AiProfile.LEGACY_V0` against silent drift.
 *
 * `LEGACY_V0` is the permanent reference opponent: every later version quotes its win rate against
 * it, so if a refactor quietly changes how V0 plays, months of numbers stop being comparable and
 * nothing says so. This test plays one fixed game and asserts the action stream hashes to a
 * constant, which is far cheaper and more honest than copying 5,575 LOC into a `frozen/v0/`
 * package that would then rot on its own.
 *
 * **The deck is deliberately all-vanilla.** Four Portal creatures with no abilities and 24
 * Mountains exercise the parts of V0 that matter — the curve, the candidate scoring, the whole
 * `CombatAdvisor` attack/block search — while touching almost none of the engine surface that
 * changes week to week. A hash over a Bloomburrow sealed game would go red every time somebody
 * implemented a card.
 *
 * **If this fails:**
 * - You changed how the AI plays. If that was intentional, it belongs behind a *new* profile,
 *   not inside `LEGACY_V0`. Re-bless [GOLDEN_HASH] only if `LEGACY_V0` genuinely had to move, and
 *   say so in the commit message — the arena numbers in `docs/ai/` were measured against the old
 *   one.
 * - You changed the engine's behaviour for a vanilla creature, a Mountain, or combat damage. Then
 *   re-blessing is correct and uninteresting.
 */
class FrozenBaselineTest : FunSpec({

    test("LEGACY_V0 plays the frozen baseline game exactly as it always has") {
        val registry = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val v0 = ArenaAgents.resolve("v0")

        val outcome = ArenaGameRunner.play(
            registry = registry,
            seat0 = v0, seat1 = v0,
            seat0Deck = FROZEN_DECK, seat1Deck = FROZEN_DECK,
            seed = FROZEN_SEED, pairId = 0, gameIndex = 0,
            maxTurns = 30,
            recordActionStream = true,
        )

        withClue(
            "LEGACY_V0's behaviour moved. Actual hash: ${outcome.actionStreamHash} " +
                "(${outcome.turns} turns, winner seat ${outcome.winnerSeat}, " +
                "life ${outcome.seat0Life}/${outcome.seat1Life}). See this test's KDoc before re-blessing."
        ) {
            outcome.actionStreamHash shouldBe GOLDEN_HASH
        }
    }
}) {
    companion object {
        private const val FROZEN_SEED = 20260727L

        /**
         * Mono-red vanilla: no triggers, no targets, no activated abilities, nothing on the stack
         * but creature spells. Pinned as a literal so a change to the sealed-pool generator or to
         * Portal's card list cannot move the baseline.
         */
        private val FROZEN_DECK = Deck(
            List(24) { "Mountain" } +
                List(4) { "Goblin Bully" } +      // {1}{R} 2/1
                List(4) { "Minotaur Warrior" } +  // {2}{R} 2/3
                List(4) { "Lizard Warrior" } +    // {3}{R} 4/2
                List(4) { "Highland Giant" }      // {2}{R}{R} 3/4
        )

        /**
         * Blessed 2026-07-27 against `AiProfile.LEGACY_V0` as pinned in Phase 1: seat 1 wins on
         * turn 10, life -8 / 16. Re-bless only for the reasons listed in this class's KDoc.
         *
         * Re-blessed 2026-07-28 for the turn-numbering change (`GameState.turnNumber` counts player
         * turns rather than rounds). **`LEGACY_V0` did not move.** The stream's only turn-number
         * carrier is its trailing `END|turns=` record, so the same game now hashes differently while
         * every action in it is identical — verified by re-running both turn-numbering schemes with
         * that record's turn count elided: both produced `8ae37a73f24d4e42`. The game itself is
         * unchanged down to the outcome: seat 1 still wins at life -8 / 16, on what is now turn 20.
         *
         * Re-blessed 2026-08-08 for the splice keyword adding `CastSpell.splicedCardIds`.
         * **`LEGACY_V0` did not move.** `TableGameRunner` records each action as its data-class
         * `toString()`, so a new `CastSpell` field appears in every recorded action — as
         * `splicedCardIds=[]` — even though no Portal card has splice and the list is empty all game.
         * Verified the same way as the 2026-07-28 entry: with `", splicedCardIds=[]"` stripped from
         * the recorded action text, this branch reproduces the previous golden `d7d1bf75e6eb1a33`
         * exactly, so the stream is identical apart from that insertion. The outcome is untouched:
         * seat 1 still wins on turn 20 at life -8 / 16.
         *
         * Note for whoever hits this next: hashing `GameAction.toString()` means *any* new field on
         * a cast/action data class moves this hash without the AI having changed. Check the outcome
         * line in the failure clue first — if turns/winner/life match the values above, you are
         * almost certainly in this benign case rather than a real behavioural drift.
         */
        private const val GOLDEN_HASH = "6ff9ded1403d59ac"
    }
}
