package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mortal's Resolve (BNG #127) — "Target creature gets +1/+1 and gains indestructible until end of
 * turn."
 */
class MortalsResolveScenarioTest : ScenarioTestBase() {
    init {
        test("the target gets +1/+1 and indestructible, surviving lethal damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Mortal's Resolve")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Mortal's Resolve", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Grizzly Bears 2/2 -> 3/3 with indestructible") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            withClue("3 damage to a 3/3 indestructible creature doesn't destroy it") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
