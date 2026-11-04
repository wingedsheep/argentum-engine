package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mummy Paramount (HOU) — "Whenever another Zombie you control enters, this creature gets +1/+1
 * until end of turn."
 */
class MummyParamountScenarioTest : ScenarioTestBase() {
    init {
        test("another Zombie entering under your control pumps it +1/+1") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mummy Paramount")
                .withCardInHand(1, "Walking Corpse")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val mummy = game.findPermanent("Mummy Paramount")!!
            game.castSpell(1, "Walking Corpse").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Walking Corpse") shouldBe true
            game.state.projectedState.getPower(mummy) shouldBe 3
            game.state.projectedState.getToughness(mummy) shouldBe 3
        }

        test("a non-Zombie creature entering doesn't pump it") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mummy Paramount")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val mummy = game.findPermanent("Mummy Paramount")!!
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.projectedState.getPower(mummy) shouldBe 2
        }
    }
}
