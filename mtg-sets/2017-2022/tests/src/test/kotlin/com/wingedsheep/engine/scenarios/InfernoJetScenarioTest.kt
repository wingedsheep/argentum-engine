package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Inferno Jet (HOU) — {5}{R} sorcery: 6 damage to target opponent or planeswalker. Cycling {2}.
 */
class InfernoJetScenarioTest : ScenarioTestBase() {
    init {
        test("deals 6 damage to target opponent") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Inferno Jet")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpellTargetingPlayer(1, "Inferno Jet", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 14
            game.isInGraveyard(1, "Inferno Jet") shouldBe true
        }

        test("can't target its own controller") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Inferno Jet")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpellTargetingPlayer(1, "Inferno Jet", 1).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 20
        }

        test("cycles for {2} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Inferno Jet").withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Inferno Jet").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Inferno Jet") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
