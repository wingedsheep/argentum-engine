package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Matter Reshaper (OGW #6) — {2}{C} Creature — Eldrazi, 3/2.
 *
 *   When this creature dies, reveal the top card of your library. You may put that card onto the
 *   battlefield if it's a permanent card with mana value 3 or less. Otherwise, put that card into
 *   your hand.
 *
 * Covers the put-onto-battlefield road and every "otherwise" road to hand the ruling names ("If you
 * don't put the card onto the battlefield for any reason"): declining, a permanent card with mana
 * value 4, a nonpermanent card, and an Aura with nothing to enchant.
 */
class MatterReshaperScenarioTest : ScenarioTestBase() {

    /** Matter Reshaper alone on the battlefield, Lightning Bolt to kill it, [top] on top of the library. */
    private fun board(top: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Matter Reshaper")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, top)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.killReshaper() {
        val reshaper = findPermanent("Matter Reshaper") ?: error("Matter Reshaper not on the battlefield")
        castSpell(1, "Lightning Bolt", reshaper).error shouldBe null
        resolveStack()
        withClue("Matter Reshaper died") { isInGraveyard(1, "Matter Reshaper") shouldBe true }
    }

    /** Declines the offer if one is made — an ineligible card may not be offered at all. */
    private fun TestGame.declineIfOffered(name: String) {
        val decision = getPendingDecision()
        if (decision is SelectCardsDecision) {
            val revealed = findCardsInLibrary(1, name).single()
            withClue("$name isn't a permanent card with mana value 3 or less") {
                selectCards(listOf(revealed)).error shouldNotBe null
            }
            selectCards(emptyList()).error shouldBe null
        }
        resolveStack()
    }

    init {
        context("Matter Reshaper") {

            test("a permanent card with mana value 3 or less may be put onto the battlefield") {
                val game = board("Grizzly Bears")
                game.killReshaper()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                choice.options shouldBe listOf(bears)
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe false
                withClue("only the top card is revealed") { game.librarySize(1) shouldBe 1 }
            }

            test("declining puts the revealed card into hand") {
                val game = board("Grizzly Bears")
                game.killReshaper()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            test("a permanent card with mana value 4 goes to hand") {
                val game = board("Hill Giant")
                game.killReshaper()
                game.declineIfOffered("Hill Giant")

                game.isInHand(1, "Hill Giant") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            test("a nonpermanent card goes to hand") {
                val game = board("Shock")
                game.killReshaper()
                game.declineIfOffered("Shock")

                game.isInHand(1, "Shock") shouldBe true
                game.librarySize(1) shouldBe 1
            }

            test("an Aura with nothing to enchant can't be put onto the battlefield, so it goes to hand") {
                val game = board("Holy Strength")
                game.killReshaper()

                val holyStrength = game.findCardsInLibrary(1, "Holy Strength").single()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(listOf(holyStrength)).error shouldBe null
                }
                game.resolveStack()

                withClue("no creature is on the battlefield for Holy Strength to enchant") {
                    game.isOnBattlefield("Holy Strength") shouldBe false
                }
                game.isInHand(1, "Holy Strength") shouldBe true
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
