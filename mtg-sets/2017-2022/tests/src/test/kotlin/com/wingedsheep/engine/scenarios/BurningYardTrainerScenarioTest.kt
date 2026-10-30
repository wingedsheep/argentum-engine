package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Burning-Yard Trainer (ELD #117) — Trample, haste. When this creature enters, another target
 * Knight you control gets +2/+2 and gains trample and haste until end of turn.
 */
class BurningYardTrainerScenarioTest : ScenarioTestBase() {

    init {
        context("Burning-Yard Trainer") {

            test("ETB pumps another Knight you control and gives it trample and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Burning-Yard Trainer")
                    .withCardOnBattlefield(1, "Knight of Grace", summoningSickness = true)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knight = game.findPermanent("Knight of Grace")!!
                game.castSpell(1, "Burning-Yard Trainer").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(knight)).error shouldBe null
                }
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(knight) shouldBe 4
                projected.getToughness(knight) shouldBe 4
                projected.hasKeyword(knight, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(knight, Keyword.HASTE) shouldBe true

                val trainer = game.findPermanent("Burning-Yard Trainer")!!
                projected.getPower(trainer) shouldBe 3
                projected.hasKeyword(trainer, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(trainer, Keyword.HASTE) shouldBe true
            }
        }
    }
}
