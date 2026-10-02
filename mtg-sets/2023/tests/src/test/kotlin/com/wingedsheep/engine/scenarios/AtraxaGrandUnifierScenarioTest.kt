package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Atraxa, Grand Unifier (ONE #196) — {3}{G}{W}{U}{B} 7/7 Legendary Creature — Phyrexian Angel.
 *
 * "When Atraxa enters, reveal the top ten cards of your library. For each card type, you may put a
 * card of that type from among the revealed cards into your hand. Put the rest on the bottom of
 * your library in a random order."
 *
 * The ruling the card hinges on: a multi-type card claims only *one* of its types, so an artifact
 * creature kept as the artifact still leaves room for a plain creature.
 */
class AtraxaGrandUnifierScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveUntilDecision() {
        var guard = 0
        while (!hasPendingDecision() && state.stack.isNotEmpty() && guard++ < 10) resolveStack()
    }

    private fun atraxaGame(vararg library: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Atraxa, Grand Unifier")
            .withLandsOnBattlefield(1, "Forest", 4)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        library.forEach { builder.withCardInLibrary(1, it) }
        val game = builder.build()
        game.castSpell(1, "Atraxa, Grand Unifier").error shouldBe null
        game.resolveUntilDecision()
        game.hasPendingDecision() shouldBe true
        return game
    }

    init {
        test("an artifact creature and a creature are kept together, one per card type") {
            val game = atraxaGame(
                "Ornithopter", "Grizzly Bears", "Lightning Bolt", "Mountain",
                "Grizzly Bears", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain",
                "Llanowar Elves",
            )
            // Artifact, creature, instant, land: four slots, and four cards can fill them.
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().maxSelections shouldBe 4

            val picks = listOf(
                game.findCardsInLibrary(1, "Ornithopter").single(),
                game.findCardsInLibrary(1, "Grizzly Bears").first(),
                game.findCardsInLibrary(1, "Lightning Bolt").single(),
                game.findCardsInLibrary(1, "Mountain").first(),
            )
            game.selectCards(picks).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Ornithopter").size shouldBe 1
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInHand(1, "Lightning Bolt").size shouldBe 1
            game.findCardsInHand(1, "Mountain").size shouldBe 1
            game.handSize(1) shouldBe 4
            // The six unkept revealed cards went to the bottom; the eleventh card was never revealed.
            game.librarySize(1) shouldBe 7
            game.findCardsInLibrary(1, "Llanowar Elves").size shouldBe 1
        }

        test("two cards that share their only type can't both be kept") {
            val game = atraxaGame(
                "Grizzly Bears", "Grizzly Bears", "Mountain", "Mountain", "Mountain",
                "Mountain", "Mountain", "Mountain", "Mountain", "Mountain",
            )
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().maxSelections shouldBe 2

            game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            game.librarySize(1) shouldBe 9
        }

        test("may keep nothing; everything goes to the bottom") {
            val game = atraxaGame("Grizzly Bears", "Mountain", "Lightning Bolt")
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 3
            game.findPermanent("Atraxa, Grand Unifier") shouldNotBe null
        }
    }
}
