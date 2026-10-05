package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AcrobaticManeuverScenarioTest : ScenarioTestBase() {
    init {
        test("exiles a creature you control, returns it as a new object, and draws a card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Acrobatic Maneuver")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)!!.has<SummoningSicknessComponent>() shouldBe false

            game.castSpell(1, "Acrobatic Maneuver", bears).error shouldBe null
            val handAfterCast = game.handSize(1)
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears")
            withClue("the creature comes back to the battlefield") { returned shouldNotBe null }
            withClue("it re-enters as a new object with summoning sickness") {
                game.state.getEntity(returned!!)!!.has<SummoningSicknessComponent>() shouldBe true
            }
            withClue("the creature is not left in exile") {
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }
            withClue("the spell draws a card") {
                game.handSize(1) shouldBe handAfterCast + 1
            }
        }

        test("cannot target a creature an opponent controls") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Acrobatic Maneuver")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opposingBears = game.findPermanent("Grizzly Bears")!!

            withClue("\"target creature you control\" rejects an opponent's creature") {
                game.castSpell(1, "Acrobatic Maneuver", opposingBears).error shouldNotBe null
            }
            game.findPermanent("Grizzly Bears") shouldBe opposingBears
        }
    }
}
