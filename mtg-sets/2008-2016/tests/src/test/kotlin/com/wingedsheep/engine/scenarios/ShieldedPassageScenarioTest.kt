package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Shielded Passage (GTC) — Prevent all damage that would be dealt to target creature this turn. */
class ShieldedPassageScenarioTest : ScenarioTestBase() {
    init {
        test("the shielded creature survives lethal damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shielded Passage")
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Shielded Passage", bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bolt's damage was prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
        }
    }
}
