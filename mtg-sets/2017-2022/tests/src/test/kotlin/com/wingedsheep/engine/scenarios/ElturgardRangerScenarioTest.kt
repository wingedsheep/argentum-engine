package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Elturgard Ranger (AFR #182) — "Reach / When this creature enters, create a 2/2 green Wolf
 * creature token."
 */
class ElturgardRangerScenarioTest : ScenarioTestBase() {

    init {
        test("entering creates a 2/2 green Wolf token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Elturgard Ranger")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Elturgard Ranger").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val ranger = game.findPermanent("Elturgard Ranger")!!
            val wolf = game.findAllPermanents("Wolf Token").single()
            val projected = game.state.projectedState
            projected.hasKeyword(ranger, Keyword.REACH) shouldBe true
            projected.getPower(wolf) shouldBe 2
            projected.getToughness(wolf) shouldBe 2
            projected.getColors(wolf) shouldBe setOf(Color.GREEN.name)
            projected.hasSubtype(wolf, "Wolf") shouldBe true
        }
    }
}
