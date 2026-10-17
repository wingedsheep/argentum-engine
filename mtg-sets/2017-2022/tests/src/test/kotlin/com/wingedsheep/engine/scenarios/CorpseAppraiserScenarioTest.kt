package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Corpse Appraiser (SNC #178) — {U}{B}{R} Creature — Vampire Rogue, 3/3.
 *
 *   When this creature enters, exile up to one target creature card from a graveyard. If a card
 *   is put into exile this way, look at the top three cards of your library, then put one of
 *   those cards into your hand and the rest into your graveyard.
 *
 * Exercises both sides of the "if a card is put into exile this way" gate: exiling a creature
 * card digs three, and choosing no target skips the dig entirely.
 */
class CorpseAppraiserScenarioTest : ScenarioTestBase() {

    init {
        context("Corpse Appraiser ETB") {

            test("exiling a creature card digs three: one to hand, the rest to graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Corpse Appraiser")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Corpse Appraiser").error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the creature card is exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                }

                val forest = game.findCardsInLibrary(1, "Forest").first()
                game.selectCards(listOf(forest)).error shouldBe null

                withClue("the chosen card goes to hand") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the other two looked-at cards go to the graveyard") {
                    game.isInGraveyard(1, "Plains") shouldBe true
                    game.isInGraveyard(1, "Wastes") shouldBe true
                }
            }

            test("choosing no target exiles nothing and skips the dig") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Corpse Appraiser")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Corpse Appraiser").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("no selection decision is pending") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the graveyard creature stays put and the library is untouched") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.findCardsInLibrary(1, "Forest").size shouldBe 1
                    game.findCardsInLibrary(1, "Plains").size shouldBe 1
                    game.findCardsInLibrary(1, "Wastes").size shouldBe 1
                }
            }
        }
    }
}
