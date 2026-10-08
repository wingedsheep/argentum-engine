package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shoot Down (BRO #190) — exile target artifact, enchantment, or creature with flying.
 */
class ShootDownScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Shoot Down")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiles a creature with flying") {
            val game = game()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Shoot Down", drake).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wind Drake") shouldBe false
            game.isInExile(2, "Wind Drake") shouldBe true
            game.isInGraveyard(2, "Wind Drake") shouldBe false
        }

        test("cannot target a creature without flying") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Shoot Down", bears).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
