package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Chimney Rabble — "Haste. When this creature enters, create a 1/1 red Phyrexian Goblin creature token."
 */
class ChimneyRabbleScenarioTest : ScenarioTestBase() {
    init {
        test("enters: creates a 1/1 Phyrexian Goblin token") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Chimney Rabble")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Chimney Rabble").error shouldBe null
            game.resolveStack()

            val rabble = game.findPermanent("Chimney Rabble")
            (rabble != null) shouldBe true
            game.state.projectedState.hasKeyword(rabble!!, Keyword.HASTE) shouldBe true
            val token = game.findPermanents("Phyrexian Goblin Token").single()
            game.state.projectedState.getPower(token) shouldBe 1
            game.state.projectedState.getToughness(token) shouldBe 1
            game.state.projectedState.getColors(token) shouldBe setOf("RED")
        }
    }
}
