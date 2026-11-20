package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thassa's Bounty — {5}{U} Sorcery (THS #67).
 *
 * "Draw three cards. Target player mills three cards."
 */
class ThassasBountyScenarioTest : ScenarioTestBase() {

    init {
        test("caster draws three and the targeted opponent mills three") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Thassa's Bounty")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Thassa's Bounty", 2).error shouldBe null
            game.resolveStack()

            withClue("caster drew three") { game.handSize(1) shouldBe 3 }
            withClue("caster's library is not milled") { game.librarySize(1) shouldBe 1 }
            withClue("target player milled three") {
                game.graveyardSize(2) shouldBe 3
                game.librarySize(2) shouldBe 1
            }
            game.isInGraveyard(1, "Thassa's Bounty") shouldBe true
        }
    }
}
