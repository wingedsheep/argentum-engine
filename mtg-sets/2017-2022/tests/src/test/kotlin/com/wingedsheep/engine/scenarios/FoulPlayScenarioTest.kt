package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Foul Play (MID #101) — destroy target creature with power 2 or less. Investigate.
 */
class FoulPlayScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Foul Play")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroys a creature with power 2 or less and investigates") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Foul Play", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Clue") shouldNotBe null
        }

        test("cannot target a creature with power 3 or greater") {
            val game = game()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Foul Play", giant).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
