package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Excavation Mole (IKO #150) — {2}{G} Creature — Mole, 3/3.
 *
 *   "Trample
 *    When this creature enters, mill three cards."
 */
class ExcavationMoleScenarioTest : ScenarioTestBase() {

    init {
        test("entering mills exactly three cards from its controller's library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Excavation Mole")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Sol Ring")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Excavation Mole").error shouldBe null
            game.resolveStack()

            val mole = game.findPermanent("Excavation Mole")
            withClue("The mole is on the battlefield with trample") {
                mole shouldNotBe null
                game.state.projectedState.hasKeyword(mole!!, Keyword.TRAMPLE) shouldBe true
            }
            withClue("Three of the four library cards were milled; the opponent's library is untouched") {
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 3
                game.librarySize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 0
            }
        }
    }
}
