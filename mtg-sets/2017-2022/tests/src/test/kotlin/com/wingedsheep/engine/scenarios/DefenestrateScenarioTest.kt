package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Defenestrate (MID #95) — destroy target creature without flying.
 */
class DefenestrateScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Defenestrate")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroys a creature without flying") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Defenestrate", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("cannot target a creature with flying") {
            val game = game()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Defenestrate", drake).error shouldNotBe null
            game.isOnBattlefield("Wind Drake") shouldBe true
        }
    }
}
