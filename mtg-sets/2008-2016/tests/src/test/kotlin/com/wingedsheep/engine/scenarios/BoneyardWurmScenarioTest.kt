package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Boneyard Wurm (ISD #171) — {1}{G} * / * Wurm.
 *
 *   Boneyard Wurm's power and toughness are each equal to the number of creature cards in your
 *   graveyard.
 */
class BoneyardWurmScenarioTest : ScenarioTestBase() {

    init {
        test("counts only creature cards in your own graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Boneyard Wurm")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(1, "Giant Growth")
                .withCardInGraveyard(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Boneyard Wurm")!!
            game.state.projectedState.getPower(wurm) shouldBe 2
            game.state.projectedState.getToughness(wurm) shouldBe 2
        }

        test("grows when one of your creatures dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Boneyard Wurm")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Boneyard Wurm")!!
            game.state.projectedState.getPower(wurm) shouldBe 1

            val cast = game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!)
            withClue("Bolt should cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(wurm) shouldBe 2
            game.state.projectedState.getToughness(wurm) shouldBe 2
        }
    }
}
