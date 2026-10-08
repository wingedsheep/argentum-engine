package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Mass Production (BRO #15) — "Create four 1/1 colorless Soldier artifact creature tokens."
 */
class MassProductionScenarioTest : ScenarioTestBase() {

    init {
        test("creates four 1/1 colorless Soldier artifact creature tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mass Production")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mass Production").error shouldBe null
            game.resolveStack()

            val tokens = game.findAllPermanents("Soldier Token")
            tokens shouldHaveSize 4
            val projected = game.state.projectedState
            tokens.forEach { token ->
                projected.isCreature(token) shouldBe true
                projected.hasType(token, "ARTIFACT") shouldBe true
                projected.hasSubtype(token, "Soldier") shouldBe true
                projected.getColors(token) shouldBe emptySet()
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
            }
        }
    }
}
