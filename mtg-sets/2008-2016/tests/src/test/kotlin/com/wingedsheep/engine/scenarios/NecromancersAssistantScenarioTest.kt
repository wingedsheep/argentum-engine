package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Necromancer's Assistant: when it enters, mill three cards.
 */
class NecromancersAssistantScenarioTest : ScenarioTestBase() {
    init {
        test("entering mills the top three cards of your library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Necromancer's Assistant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Plains")
                .build()
            game.castSpell(1, "Necromancer's Assistant").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Necromancer's Assistant") shouldBe true
            game.graveyardSize(1) shouldBe 3
            game.librarySize(1) shouldBe 1
        }
    }
}
