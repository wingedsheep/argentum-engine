package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Deadbridge Chant — "When this enchantment enters, mill ten cards. At the beginning of your
 * upkeep, choose a card at random in your graveyard. If it's a creature card, put it onto the
 * battlefield. Otherwise, put it into your hand."
 *
 * The random pick is over the whole graveyard and the branch reads the card picked: a lone
 * creature card comes back onto the battlefield, a lone non-creature card goes to hand, and in a
 * mixed graveyard exactly one card leaves — to the zone its own type dictates.
 */
class DeadbridgeChantScenarioTest : ScenarioTestBase() {

    init {
        test("entering mills ten cards") {
            var builder = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Deadbridge Chant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(12) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
            val game = builder.build()

            game.castSpell(1, "Deadbridge Chant").error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 10
            game.librarySize(1) shouldBe 2
        }

        fun upkeepWithGraveyard(vararg graveyard: String): TestGame {
            var builder = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Deadbridge Chant")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            graveyard.forEach { builder = builder.withCardInGraveyard(1, it) }
            repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            return game
        }

        test("a creature card chosen at random is put onto the battlefield") {
            val game = upkeepWithGraveyard("Grizzly Bears")
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.graveyardSize(1) shouldBe 0
        }

        test("a non-creature card chosen at random is put into your hand") {
            val game = upkeepWithGraveyard("Lightning Bolt")
            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.graveyardSize(1) shouldBe 0
        }

        test("in a mixed graveyard exactly one card leaves, to the zone its type dictates") {
            val game = upkeepWithGraveyard("Grizzly Bears", "Lightning Bolt")
            game.graveyardSize(1) shouldBe 1
            val bearsReturned = game.isOnBattlefield("Grizzly Bears")
            val boltReturned = game.isInHand(1, "Lightning Bolt")
            (bearsReturned != boltReturned) shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.isInGraveyard(1, if (bearsReturned) "Lightning Bolt" else "Grizzly Bears") shouldBe true
        }

        test("an empty graveyard does nothing") {
            val game = upkeepWithGraveyard()
            game.graveyardSize(1) shouldBe 0
            game.isOnBattlefield("Deadbridge Chant") shouldBe true
        }
    }
}
