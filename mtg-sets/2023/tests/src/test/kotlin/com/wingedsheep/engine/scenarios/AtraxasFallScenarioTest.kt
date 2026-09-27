package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Atraxa's Fall — "Destroy target artifact, battle, enchantment, or creature with flying."
 */
class AtraxasFallScenarioTest : ScenarioTestBase() {

    private fun board(target: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Atraxa's Fall")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(2, target)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroys a battle") {
            // Opponent controls the Siege; SBAs give it a protector (Player).
            val game = board("Invasion of Innistrad")
            game.checkStateBasedActions()
            game.castSpell(1, "Atraxa's Fall", game.findPermanent("Invasion of Innistrad")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Invasion of Innistrad") shouldBe false
            game.isInGraveyard(2, "Invasion of Innistrad") shouldBe true
        }

        test("destroys a creature with flying") {
            val game = board("Serra Angel")
            game.castSpell(1, "Atraxa's Fall", game.findPermanent("Serra Angel")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Serra Angel") shouldBe true
        }

        test("destroys an artifact") {
            val game = board("Mask of Memory")
            game.castSpell(1, "Atraxa's Fall", game.findPermanent("Mask of Memory")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Mask of Memory") shouldBe true
        }

        test("can't target a creature without flying") {
            val game = board("Grizzly Bears")
            game.castSpell(1, "Atraxa's Fall", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
