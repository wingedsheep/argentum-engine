package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Niv-Mizzet Reborn (WAR #208) — {W}{U}{B}{R}{G} Legendary Creature — Dragon Avatar 6/6.
 *
 * "Flying. When Niv-Mizzet enters, reveal the top ten cards of your library. For each color pair,
 *  choose a card that's exactly those colors from among them. Put the chosen cards into your hand
 *  and the rest on the bottom of your library in a random order."
 *
 * The top ten hold two white-blue cards (one pick, so the only prompt), one blue-red and one
 * black-red card (taken without asking), a five-color card and a mono-colored card (never
 * eligible), and four Islands. An eleventh card below them must end up on top of the library.
 */
class NivMizzetRebornScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Niv-Mizzet Reborn")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Dovin's Veto")        // W/U
        .withCardInLibrary(1, "Time Wipe")           // W/U
        .withCardInLibrary(1, "Ral's Outburst")      // U/R
        .withCardInLibrary(1, "Angrath's Rampage")   // B/R
        .withCardInLibrary(1, "Niv-Mizzet, Supreme") // five colors
        .withCardInLibrary(1, "Grizzly Bears")       // mono-green
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Mayhem Devil")        // 11th card, not revealed
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Niv-Mizzet Reborn") {

            test("takes one card of each color pair present; the rest go to the bottom") {
                val game = game()
                val devil = game.findCardsInLibrary(1, "Mayhem Devil").single()
                game.castSpell(1, "Niv-Mizzet Reborn").error shouldBe null
                game.resolveStack()

                // Only white-blue has two candidates, so it is the only question asked.
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val veto = game.findCardsInLibrary(1, "Dovin's Veto").single()
                val wipe = game.findCardsInLibrary(1, "Time Wipe").single()
                decision.options.filter { it !in decision.nonSelectableOptions } shouldContainExactlyInAnyOrder
                    listOf(veto, wipe)
                decision.maxSelections shouldBe 1
                game.selectCards(listOf(veto)).error shouldBe null
                game.resolveStack()

                withClue("no further pair offers a choice") {
                    game.getPendingDecision() shouldBe null
                }
                game.isInHand(1, "Dovin's Veto") shouldBe true
                game.isInHand(1, "Ral's Outburst") shouldBe true
                game.isInHand(1, "Angrath's Rampage") shouldBe true
                game.handSize(1) shouldBe 3
                withClue("only one card per color pair: the second white-blue card stays behind") {
                    game.isInHand(1, "Time Wipe") shouldBe false
                }
                game.librarySize(1) shouldBe 8
                withClue("the seven unchosen revealed cards went under the unrevealed eleventh card") {
                    game.state.getLibrary(game.player1Id).first() shouldBe devil
                }
                game.isOnBattlefield("Niv-Mizzet Reborn") shouldBe true
            }
        }
    }
}
