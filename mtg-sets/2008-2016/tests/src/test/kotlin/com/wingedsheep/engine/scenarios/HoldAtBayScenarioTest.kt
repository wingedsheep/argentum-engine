package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hold at Bay (BNG #18) — "Prevent the next 7 damage that would be dealt to any target this turn."
 */
class HoldAtBayScenarioTest : ScenarioTestBase() {
    init {
        test("a shielded creature survives damage that would otherwise kill it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Hold at Bay")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Hold at Bay", bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the 3 damage was prevented by the 7-point shield") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("a shielded player takes no damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Hold at Bay")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Hold at Bay", 2).error shouldBe null
            game.resolveStack()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
        }
    }
}
