package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mortal's Ardor (BNG #20) — "Target creature gets +1/+1 and gains lifelink until end of turn."
 */
class MortalsArdorScenarioTest : ScenarioTestBase() {
    init {
        test("the target gets +1/+1 and lifelink, and its combat damage gains its controller life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Mortal's Ardor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Mortal's Ardor", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Grizzly Bears 2/2 -> 3/3 with lifelink") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
