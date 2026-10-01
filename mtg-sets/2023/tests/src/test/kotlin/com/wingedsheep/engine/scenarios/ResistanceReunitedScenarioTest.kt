package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Resistance Reunited (ONE #31) — {1}{W} Instant.
 * "Target creature gets +2/+2 until end of turn.
 *  Equipped creatures you control gain indestructible until end of turn."
 */
class ResistanceReunitedScenarioTest : ScenarioTestBase() {
    init {
        test("pumps the target; only equipped creatures you control gain indestructible") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Resistance Reunited")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Hill Giant")       // unequipped target, 3/3
                .withCardOnBattlefield(1, "Grizzly Bears")    // equipped, mine
                .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")  // equipped, opponent's
                .withCardAttachedTo(2, "Leonin Scimitar", "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Resistance Reunited", giant).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(giant) shouldBe 5
            projected.getToughness(giant) shouldBe 5
            projected.hasKeyword(giant, Keyword.INDESTRUCTIBLE) shouldBe false

            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.getPower(bears) shouldBe 4 // 2 + Bonesplitter's +2, no pump
            projected.hasKeyword(courser, Keyword.INDESTRUCTIBLE) shouldBe false
        }

        test("targeting an equipped creature gives it both +2/+2 and indestructible") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Resistance Reunited")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Leonin Scimitar", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Resistance Reunited", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 5 // 2 + Scimitar +1 + pump +2
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
