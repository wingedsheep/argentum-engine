package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scorchmark (RNA #113) — 2 damage to target creature; if it would die this turn, exile it instead.
 */
class ScorchmarkScenarioTest : ScenarioTestBase() {
    init {
        test("a creature killed by the damage is exiled, not put into the graveyard") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Scorchmark")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Scorchmark", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
        }
    }
}
