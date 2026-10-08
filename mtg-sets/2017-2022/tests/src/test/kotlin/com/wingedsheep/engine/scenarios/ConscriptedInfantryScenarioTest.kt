package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Conscripted Infantry (BRO #129) — when it dies, create a 1/1 colorless Soldier artifact
 * creature token.
 */
class ConscriptedInfantryScenarioTest : ScenarioTestBase() {

    init {
        test("dying creates a 1/1 colorless Soldier artifact creature token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Conscripted Infantry")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val infantry = game.findPermanent("Conscripted Infantry")!!
            game.castSpell(1, "Lightning Bolt", infantry).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Conscripted Infantry") shouldBe true
            val tokens = game.findPermanents("Soldier Token")
            tokens.size shouldBe 1
            val token = tokens.single()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.isCreature(token) shouldBe true
            projected.hasType(token, "ARTIFACT") shouldBe true
            projected.getColors(token).isEmpty() shouldBe true
        }
    }
}
