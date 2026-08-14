package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * `AiProfile.sequenceLandsByUsableMana`: a tapped land costs you only the mana you would have spent.
 *
 * Asserted on [BoardPresence] alone rather than through a whole evaluator, for
 * `CardAdvantageLandDropTest`'s reason — the claim is about this one feature, and a composite would
 * let [Tempo] cover a wrong number here with a right one there. That the term moves the *decision*
 * is `PuzzleSuiteTest`'s `sequencing-07` / `sequencing-08`, which are deliberately the same board
 * with opposite right answers.
 *
 * Every position below is three lands and one card in hand, because that is the smallest board on
 * which "which land" is a different question from "how many lands".
 */
class BoardPresenceLandSequencingTest : ScenarioTestBase() {

    init {
        // Every constant here is a sum of tenths, so the assertions compare accumulated binary
        // floats — 0.6 + 0.6 + 0.3 + 0.3 is not 0.6 * 3. The tolerance is about the arithmetic, not
        // about the term being approximately right.
        val intents = IntentCatalog.of(cardRegistry)

        /** [BoardPresence] for seat 1, with the land-sequencing term on or off. */
        fun GameState.presence(on: Boolean): Double {
            val seat = turnOrder[0]
            return BoardPresence.score(this, projectedState, seat, intents, on)
        }

        /** Three lands — [tapped] of them tapped — and [inHand] as the whole hand. */
        fun board(tapped: Int, inHand: String): GameState {
            var builder = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 3 - tapped)
            repeat(tapped) { builder = builder.withCardOnBattlefield(1, "Mountain", tapped = true) }
            return builder.withCardInHand(1, inHand).build().state
        }

        // ── 1. The refund: mana nobody could have spent was never lost ──

        test("a tapped land whose mana nothing in hand could use is worth a full land") {
            // Hill Giant is {3}{R}: untapping the third land still leaves it uncastable, so being
            // tapped costs this player nothing this turn.
            withClue("one tapped land, a 4-drop in hand") {
                board(tapped = 1, inHand = "Hill Giant").presence(on = true) shouldBe
                    (board(tapped = 0, inHand = "Hill Giant").presence(on = true) plusOrMinus EPSILON)
            }
        }

        test("a tapped land whose mana would have cast something is still charged") {
            // Gray Ogre is {2}{R}: the third land is exactly what makes it castable, so tapped is a
            // real loss and the untapped board has to score higher.
            board(tapped = 0, inHand = "Gray Ogre").presence(on = true) shouldBeGreaterThan
                board(tapped = 1, inHand = "Gray Ogre").presence(on = true)
        }

        // ── 2. The debt: a tapland in hand is a turn of mana still owed ──

        test("holding a land that always enters tapped is worse than holding a basic") {
            board(tapped = 0, inHand = "Shivan Oasis").presence(on = true) shouldBeLessThan
                board(tapped = 0, inHand = "Mountain").presence(on = true)
        }

        test("a land that only *might* enter tapped is not charged — the analyzer declines") {
            // Rootbound Crag enters untapped if you control a Mountain or a Forest. Pricing that as
            // an unconditional tapland would rank the better card below the worse one, so
            // `CardIntent.entersTapped` reads false and this scores exactly like a basic.
            board(tapped = 0, inHand = "Rootbound Crag").presence(on = true) shouldBe
                (board(tapped = 0, inHand = "Mountain").presence(on = true) plusOrMinus EPSILON)
        }

        // ── 3. The ordering the whole design rests on ──

        test("the hand debt never outweighs mana that is live this turn") {
            // The pair `sequencing-08` scores, stated as an inequality. Playing the basic leaves a
            // tapland in hand (a debt) but keeps three untapped mana for the Ogre; playing the
            // tapland saves the debt and strands the spell. Live mana has to win — if this flips,
            // the AI starts dumping taplands on turns it needed the mana, which is the same mistake
            // in the opposite direction.
            val playedTheBasic = board(tapped = 0, inHand = "Shivan Oasis")
            val playedTheTapland = board(tapped = 1, inHand = "Gray Ogre")
            playedTheBasic.presence(on = true) shouldBeGreaterThan playedTheTapland.presence(on = true)
        }

        // ── 4. Off is off ──

        test("with the term off, every position scores exactly as it did before") {
            listOf(
                board(tapped = 0, inHand = "Mountain"),
                board(tapped = 0, inHand = "Shivan Oasis"),
                board(tapped = 1, inHand = "Hill Giant"),
                board(tapped = 1, inHand = "Gray Ogre"),
            ).forEach { state ->
                withClue("a tapped land is a flat 0.3 and a hand is never read") {
                    state.presence(on = false) shouldBe
                        (BoardPresence.score(state, state.projectedState, state.turnOrder[0], intents)
                            plusOrMinus EPSILON)
                }
            }
        }

        // ── 5. Fair play ──

        test("the opponent's hand is never read, however tempting") {
            // Both halves of the term read hand *contents* — mana values, and which lands enter
            // tapped. The AI is entitled to its own and to nobody else's; reading across the table
            // would be the hidden-information cheat Phase 8's determinizer exists to remove.
            val opponentHoldsATapland = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Mountain")
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .withCardInHand(2, "Shivan Oasis")
                .withCardInHand(2, "Hill Giant")
                .build().state
            val opponentHoldsABasic = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Mountain")
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Gray Ogre")
                .build().state
            opponentHoldsATapland.presence(on = true) shouldBe
                (opponentHoldsABasic.presence(on = true) plusOrMinus EPSILON)
        }

        test("an empty hand and an empty board do not throw") {
            val state = scenario().withPlayers().build().state
            state.presence(on = true) shouldBe (state.presence(on = false) plusOrMinus EPSILON)
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
