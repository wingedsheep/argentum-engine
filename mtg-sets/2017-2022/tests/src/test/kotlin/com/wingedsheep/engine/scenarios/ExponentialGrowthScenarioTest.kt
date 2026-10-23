package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Exponential Growth doubles the target's power X times in sequence: each repeated +N/+0 reads
 * the power the previous doubling left, so a 2/2 at X = 3 ends as a 16/2 (the card's ruling).
 */
class ExponentialGrowthScenarioTest : ScenarioTestBase() {
    init {
        test("X = 3 doubles a 2/2's power three times in sequence, making it 16/2") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Exponential Growth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castXSpell(1, "Exponential Growth", xValue = 3, targetId = bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 16
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("X = 0 leaves the creature's power unchanged") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Exponential Growth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castXSpell(1, "Exponential Growth", xValue = 0, targetId = bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("the bonus ends at end of turn") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Exponential Growth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castXSpell(1, "Exponential Growth", xValue = 1, targetId = bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 4

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
