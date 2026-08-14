package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Torch the Witness (MKM #146) — {X}{R} Sorcery.
 *
 * "Torch the Witness deals twice X damage to target creature. If excess damage was dealt to that
 *  creature this way, investigate."
 *
 * Two independent claims, and the tests are chosen to sit exactly on the boundary between them:
 *
 *  - **twice X.** The cost has a single {X}, so X = 1 must deal 2 and X = 2 must deal 4. A
 *    "{X}{X}"-style misreading or a missing multiplier both survive a single-value test, so the
 *    cases below use two different values of X against the same 2/2.
 *  - **excess, not merely lethal.** 2 damage into a 2/2 is exactly lethal and makes *no* Clue;
 *    4 damage into the same 2/2 is two points of excess and makes one. An implementation that
 *    checked "did the creature die?" instead of "was excess dealt?" passes the kill assertions in
 *    both tests and fails only on the Clue count — which is why the Clue count is the assertion
 *    that matters here.
 *
 * X = 0 is the degenerate third case: a legal cast that deals nothing, kills nothing, and — since
 * zero damage cannot be excess — leaves no Clue.
 */
class TorchTheWitnessScenarioTest : ScenarioTestBase() {

    private fun clues(game: TestGame): Int = game.findPermanents("Clue").size

    private fun boardWithBears(): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Torch the Witness")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("X=2 deals 4 to a 2/2 — two points of excess, so it investigates") {
            val game = boardWithBears()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castXSpell(1, "Torch the Witness", xValue = 2, targetId = bears).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            withClue("twice X = 4 damage kills the 2/2") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("4 into a 2/2 is 2 points past lethal — excess was dealt") {
                clues(game) shouldBe 1
            }
        }

        test("X=1 deals exactly lethal 2 — it kills, but there is no excess and no Clue") {
            val game = boardWithBears()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castXSpell(1, "Torch the Witness", xValue = 1, targetId = bears).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            withClue("twice X = 2 damage is exactly lethal to a 2/2") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("exactly lethal is not excess (CR 120.10) — no Clue") {
                clues(game) shouldBe 0
            }
        }

        test("X=0 is a legal cast that does nothing at all") {
            val game = boardWithBears()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castXSpell(1, "Torch the Witness", xValue = 0, targetId = bears).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            withClue("0 damage leaves the creature alive") {
                game.findPermanent("Grizzly Bears") shouldBe bears
            }
            withClue("and zero damage can never be excess") {
                clues(game) shouldBe 0
            }
        }
    }
}
