package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Time Wipe (WAR) — "Return a creature you control to its owner's hand, then destroy all
 * creatures." The bounce is chosen on resolution, only among your creatures, and happens first.
 */
class TimeWipeScenarioTest : ScenarioTestBase() {

    init {
        context("Time Wipe") {
            test("you choose one of your creatures to save, then every other creature is destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Time Wipe")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bog Imp")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Time Wipe").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("only your own creatures are offered") {
                    decision.options.contains(game.findPermanent("Hill Giant")!!) shouldBe false
                }
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Bog Imp") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("with no creature of your own, it still destroys all creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Time Wipe")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Time Wipe").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
