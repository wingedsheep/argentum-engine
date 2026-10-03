package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Imperial Aerosaur (XLN #14) — "Flying. When this creature enters, another target creature you
 * control gets +1/+1 and gains flying until end of turn."
 */
class ImperialAerosaurScenarioTest : ScenarioTestBase() {
    init {
        test("the enters trigger pumps and grants flying to another creature you control until end of turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Imperial Aerosaur")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Imperial Aerosaur").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val aerosaur = game.findPermanent("Imperial Aerosaur")!!
            game.state.projectedState.hasKeyword(aerosaur, Keyword.FLYING) shouldBe true
            withClue("Grizzly Bears is a 3/3 flier this turn") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }
            withClue("the Aerosaur can't target itself") {
                game.state.projectedState.getPower(aerosaur) shouldBe 3
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            withClue("the pump wears off") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
            }
        }
    }
}
