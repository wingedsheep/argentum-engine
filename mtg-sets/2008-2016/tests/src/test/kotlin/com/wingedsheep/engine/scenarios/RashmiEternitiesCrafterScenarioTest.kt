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
 * Rashmi, Eternities Crafter (KLD #184) — {2}{G}{U} Legendary Creature — Elf Druid, 2/3.
 *
 *   Whenever you cast your first spell each turn, reveal the top card of your library. You may
 *   cast it without paying its mana cost if it's a spell with lesser mana value. If you don't cast
 *   it, put it into your hand.
 *
 * Covers the free cast of a cheaper spell (and that it, the second spell, doesn't re-trigger), the
 * three "you don't cast it" roads to hand — equal mana value, a land, declining — and the ruling
 * that a countered triggering spell's mana value is still the one it had on the stack.
 */
class RashmiEternitiesCrafterScenarioTest : ScenarioTestBase() {

    /** Rashmi in play, Hill Giant (mana value 4) in hand, [top] on top of the library. */
    private fun board(top: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Rashmi, Eternities Crafter")
        .withCardInHand(1, "Hill Giant")
        .withCardInHand(1, "Counterspell")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withLandsOnBattlefield(1, "Island", 5)
        .withCardInLibrary(1, top)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Rashmi, Eternities Crafter") {

            test("a cheaper revealed spell is cast for free, and that second spell doesn't trigger again") {
                val game = board("Grizzly Bears")
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                choice.options shouldBe listOf(bears)
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears was cast from the library and resolved") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
                game.isOnBattlefield("Hill Giant") shouldBe true
                withClue("the free cast is the turn's second spell, so the Island under it stays put") {
                    game.hasPendingDecision() shouldBe false
                    game.librarySize(1) shouldBe 1
                    game.isInHand(1, "Island") shouldBe false
                }
            }

            test("a revealed spell with equal mana value can't be cast and goes to hand") {
                val game = board("Hill Giant")
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    val revealed = game.findCardsInLibrary(1, "Hill Giant").single()
                    withClue("mana value 4 isn't lesser than 4") {
                        game.selectCards(listOf(revealed)).error shouldNotBe null
                    }
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.isInHand(1, "Hill Giant") shouldBe true
                game.findCardsInLibrary(1, "Hill Giant") shouldBe emptyList()
                game.librarySize(1) shouldBe 1
            }

            test("a revealed land goes to hand") {
                val game = board("Forest")
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.isInHand(1, "Forest") shouldBe true
                game.isOnBattlefield("Forest") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            test("declining the free cast puts the revealed card into hand") {
                val game = board("Grizzly Bears")
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            test("the triggering spell's mana value is kept when it is countered before the trigger resolves") {
                val game = board("Grizzly Bears")
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.castSpellTargetingStackSpell(1, "Counterspell", "Hill Giant").error shouldBe null
                game.resolveStack()

                withClue("Hill Giant was countered before Rashmi's trigger resolved") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                withClue("Grizzly Bears is still a spell with lesser mana value than the countered Hill Giant") {
                    choice.options shouldBe listOf(bears)
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
