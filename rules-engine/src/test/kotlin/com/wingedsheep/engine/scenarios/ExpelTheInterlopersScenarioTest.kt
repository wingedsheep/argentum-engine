package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Expel the Interlopers (WOE #13) — {3}{W}{W} Sorcery.
 * "Choose a number between 0 and 10. Destroy all creatures with power greater than or equal to
 * the chosen number."
 *
 * Covers the new [com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtLeastX] predicate:
 * the chosen number is stamped onto the resolution context as X and the non-targeted wipe filters
 * against it. Both endpoints of the printed range are exercised (0 sweeps the board, 10 spares it)
 * along with the ordinary partial sweep, and the wipe is checked to be symmetrical.
 */
class ExpelTheInterlopersScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Expel the Interlopers")
            .withLandsOnBattlefield(1, "Plains", 5)
            // 1/1 (yours), 2/2 (yours), 3/3 (theirs) — the wipe is symmetrical.
            .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the chosen number sets the power threshold — 2 kills the 2/2 and 3/3, spares the 1/1") {
            val game = board()
            val projected = game.state.projectedState
            withClue("board precondition") {
                projected.getPower(game.findPermanent("Llanowar Elves")!!) shouldBe 1
                projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
                projected.getPower(game.findPermanent("Centaur Courser")!!) shouldBe 3
            }

            withClue("cast should succeed") {
                game.castSpell(1, "Expel the Interlopers").error shouldBe null
            }
            game.resolveStack()
            game.chooseNumber(2)
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Centaur Courser") shouldBe false
        }

        test("choosing 0 destroys every creature, including your own") {
            val game = board()
            game.castSpell(1, "Expel the Interlopers").error shouldBe null
            game.resolveStack()
            game.chooseNumber(0)
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Centaur Courser") shouldBe false
        }

        test("choosing 10 destroys nothing when no creature has power 10 or greater") {
            val game = board()
            game.castSpell(1, "Expel the Interlopers").error shouldBe null
            game.resolveStack()
            game.chooseNumber(10)
            game.resolveStack()

            withClue("an out-of-reach threshold must not fail open into a board wipe") {
                game.isOnBattlefield("Llanowar Elves") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
        }
    }
}
