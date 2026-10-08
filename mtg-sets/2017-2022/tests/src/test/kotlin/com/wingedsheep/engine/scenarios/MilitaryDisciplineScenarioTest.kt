package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Military Discipline (BRO #17) — Flash Aura: on entering, the enchanted creature gains first
 * strike until end of turn; it permanently gets +1/+0.
 */
class MilitaryDisciplineScenarioTest : ScenarioTestBase() {

    init {
        test("enchanted creature gets +1/+0 and first strike until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Military Discipline")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Military Discipline", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Military Discipline") shouldBe true
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
