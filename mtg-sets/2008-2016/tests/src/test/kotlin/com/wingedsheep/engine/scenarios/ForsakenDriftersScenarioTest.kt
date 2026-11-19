package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Forsaken Drifters (BNG #72) — "When this creature dies, mill four cards."
 */
class ForsakenDriftersScenarioTest : ScenarioTestBase() {
    init {
        test("dying mills its controller four cards") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Forsaken Drifters")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val drifters = game.findPermanent("Forsaken Drifters")!!

            game.castSpell(1, "Lightning Bolt", drifters).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Forsaken Drifters") shouldBe true
            withClue("four of the five library cards were milled") {
                game.librarySize(1) shouldBe 1
                game.findCardsInGraveyard(1, "Swamp").size shouldBe 4
            }
        }
    }
}
