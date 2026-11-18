package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pin to the Earth (JOU #45) — {1}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets -6/-0.
 */
class PinToTheEarthScenarioTest : ScenarioTestBase() {
    init {
        test("the enchanted creature loses 6 power and keeps its toughness") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Pin to the Earth")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Pin to the Earth", wurm).error shouldBe null
            game.resolveStack()

            withClue("Pin to the Earth is on the battlefield") {
                game.isOnBattlefield("Pin to the Earth") shouldBe true
            }
            withClue("the 6/4 Craw Wurm is now a 0/4") {
                game.state.projectedState.getPower(wurm) shouldBe 0
                game.state.projectedState.getToughness(wurm) shouldBe 4
            }
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
