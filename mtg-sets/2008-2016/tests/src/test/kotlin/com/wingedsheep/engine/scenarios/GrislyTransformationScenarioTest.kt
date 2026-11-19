package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grisly Transformation (BNG #74) — "When this Aura enters, draw a card. Enchanted creature has
 * intimidate."
 */
class GrislyTransformationScenarioTest : ScenarioTestBase() {
    init {
        test("entering draws a card and the enchanted creature gains intimidate") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Grisly Transformation")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Grisly Transformation", bears).error shouldBe null
            game.resolveStack()

            withClue("the Aura's entry drew a card") { game.isInHand(1, "Island") shouldBe true }
            game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
        }
    }
}
