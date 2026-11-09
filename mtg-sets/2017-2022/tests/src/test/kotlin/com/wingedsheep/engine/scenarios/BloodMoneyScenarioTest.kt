package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Blood Money {5}{B}{B} — Sorcery.
 *
 * "Destroy all creatures. For each nontoken creature destroyed this way, you create a tapped
 *  Treasure token."
 *
 * Pays only for creatures that were destroyed *and* nontoken: a token (which ceases to exist) and
 * an indestructible creature (not destroyed at all) earn nothing.
 */
class BloodMoneyScenarioTest : ScenarioTestBase() {
    init {
        context("Blood Money") {
            test("one tapped Treasure per nontoken creature actually destroyed") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Blood Money")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(2, "Darksteel Colossus")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood Money").error shouldBe null
                game.resolveStack()

                withClue("the two ordinary creatures died") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("the indestructible creature survived") { game.isOnBattlefield("Darksteel Colossus") shouldBe true }
                withClue("the token is gone") { game.findAllPermanents("Grizzly Bears").size shouldBe 0 }

                val treasures = game.findPermanents("Treasure")
                withClue("2 nontoken creatures destroyed -> 2 Treasures (not 3 for the token, not 4 for Colossus)") {
                    treasures.size shouldBe 2
                }
                treasures.forEach { game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
            }
        }
    }
}
