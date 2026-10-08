package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Burrowing Razormaw (BRO #173) — "When this creature dies, mill four cards."
 */
class BurrowingRazormawScenarioTest : ScenarioTestBase() {

    init {
        test("dying mills its controller's top four cards") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Burrowing Razormaw")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
            repeat(5) { builder.withCardInLibrary(1, "Forest") }
            val game = builder
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Murder", game.findPermanent("Burrowing Razormaw")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Burrowing Razormaw") shouldBe true
            game.librarySize(1) shouldBe 1
            game.findCardsInGraveyard(1, "Forest").size shouldBe 4
            game.librarySize(2) shouldBe 1
        }
    }
}
