package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class CrushingVinesScenarioTest : ScenarioTestBase() {
    init {
        fun game() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Crushing Vines")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardOnBattlefield(2, "Serra Angel")
            .withCardOnBattlefield(2, "Jayemdae Tome")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("first mode destroys target creature with flying") {
            val game = game()
            val angel = game.findPermanent("Serra Angel")!!
            val result = game.castSpellWithMode(1, "Crushing Vines", 0, angel)
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.isInGraveyard(2, "Serra Angel") shouldBe true
            game.isOnBattlefield("Jayemdae Tome") shouldBe true
        }

        test("first mode can't target a creature without flying") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val result = game.castSpellWithMode(1, "Crushing Vines", 0, bears)
            withClue("casting at a non-flier should be rejected") { (result.error != null) shouldBe true }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("second mode destroys target artifact") {
            val game = game()
            val tome = game.findPermanent("Jayemdae Tome")!!
            val result = game.castSpellWithMode(1, "Crushing Vines", 1, tome)
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.isInGraveyard(2, "Jayemdae Tome") shouldBe true
            game.isOnBattlefield("Serra Angel") shouldBe true
        }
    }
}
