package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ironwright's Cleansing (EMN #32) — {2}{W} Sorcery.
 *
 *   Exile target artifact or enchantment.
 *
 * Exiles (not destroys) either an artifact or an enchantment, and can't be aimed at a plain creature.
 */
class IronwrightsCleansingScenarioTest : ScenarioTestBase() {

    init {
        test("exiles a target enchantment") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ironwright's Cleansing")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val anthem = game.findPermanent("Glorious Anthem")!!
            game.castSpell(1, "Ironwright's Cleansing", anthem).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Glorious Anthem") shouldBe false
            game.isInExile(2, "Glorious Anthem") shouldBe true
            game.isInGraveyard(2, "Glorious Anthem") shouldBe false
        }

        test("exiles a target artifact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ironwright's Cleansing")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val thopter = game.findPermanent("Ornithopter")!!
            game.castSpell(1, "Ironwright's Cleansing", thopter).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Ornithopter") shouldBe true
        }

        test("cannot target a non-artifact creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ironwright's Cleansing")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Ironwright's Cleansing", bears).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
