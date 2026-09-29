package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class CinderStormScenarioTest : ScenarioTestBase() {
    init {
        test("deals seven damage to a player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cinder Storm")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Cinder Storm", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 13
            game.getLifeTotal(1) shouldBe 20
            game.isInGraveyard(1, "Cinder Storm") shouldBe true
        }

        test("deals lethal damage to a creature without damaging its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cinder Storm")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cinder Storm", game.findPermanent("Craw Wurm")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
