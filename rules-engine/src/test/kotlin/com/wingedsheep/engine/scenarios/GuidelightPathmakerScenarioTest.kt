package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Guidelight Pathmaker (DFT #206).
 *
 * Guidelight Pathmaker {4}{W}{U} — Artifact — Vehicle 6/5
 * Vigilance
 * When this Vehicle enters, you may search your library for an artifact card and reveal it. Put it
 * onto the battlefield if its mana value is 2 or less. Otherwise, put it into your hand. If you
 * search your library this way, shuffle.
 * Crew 2
 *
 * The load-bearing claim is the **branch on the found card's mana value**: one search, two
 * destinations. Grim Bauble ({B}, mana value 1) must land on the battlefield; Rover Blades ({3},
 * mana value 3) must land in hand. The third case is the "you may" — declining must move nothing.
 */
class GuidelightPathmakerScenarioTest : ScenarioTestBase() {

    init {
        context("Guidelight Pathmaker") {

            test("a mana value 2 or less artifact goes onto the battlefield") {
                val game = pathmakerGame()
                val bauble = game.findCardsInLibrary(1, "Grim Bauble").single()

                game.castSpell(1, "Guidelight Pathmaker").error shouldBe null
                game.resolveStack()

                withClue("the trigger should first ask whether to search") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.selectCards(listOf(bauble)).error shouldBe null
                game.resolveStack()

                withClue("mana value 1 ≤ 2, so it enters the battlefield") {
                    game.isOnBattlefield("Grim Bauble") shouldBe true
                    game.isInHand(1, "Grim Bauble") shouldBe false
                }
            }

            test("an artifact with mana value 3 or more goes to hand instead") {
                val game = pathmakerGame()
                val blades = game.findCardsInLibrary(1, "Rover Blades").single()

                game.castSpell(1, "Guidelight Pathmaker").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.selectCards(listOf(blades)).error shouldBe null
                game.resolveStack()

                withClue("mana value 3 > 2, so it goes to hand, not the battlefield") {
                    game.isInHand(1, "Rover Blades") shouldBe true
                    game.isOnBattlefield("Rover Blades") shouldBe false
                }
            }

            test("declining the search moves nothing") {
                val game = pathmakerGame()
                val librarySizeBefore = game.librarySize(1)

                game.castSpell(1, "Guidelight Pathmaker").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("the Vehicle still entered") {
                    game.isOnBattlefield("Guidelight Pathmaker") shouldBe true
                }
                withClue("but no card left the library and neither artifact reached hand or battlefield") {
                    game.librarySize(1) shouldBe librarySizeBefore
                    game.isInHand(1, "Grim Bauble") shouldBe false
                    game.isInHand(1, "Rover Blades") shouldBe false
                    game.isOnBattlefield("Grim Bauble") shouldBe false
                    game.isOnBattlefield("Rover Blades") shouldBe false
                }
            }
        }
    }

    /** Pathmaker in hand, six lands to cast it, and one cheap + one expensive artifact to find. */
    private fun pathmakerGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Guidelight Pathmaker")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withLandsOnBattlefield(1, "Island", 3)
            .withCardInLibrary(1, "Grim Bauble")
            .withCardInLibrary(1, "Rover Blades")
        repeat(6) { builder.withCardInLibrary(2, "Grizzly Bears") }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
