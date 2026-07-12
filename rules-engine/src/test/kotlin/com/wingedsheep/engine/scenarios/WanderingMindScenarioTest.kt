package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wandering Mind (VOW #251) — {1}{U}{R} Creature — Horror, 2/1, flying.
 *
 *   When this creature enters, look at the top six cards of your library. You may reveal a
 *   noncreature, nonland card from among them and put it into your hand. Put the rest on the
 *   bottom of your library in a random order.
 *
 * Exercises the ETB look-and-maybe-reveal pipeline: only the noncreature, nonland card among the
 * top six is a legal reveal target, and taking it moves it to hand while the rest go to the
 * bottom.
 */
class WanderingMindScenarioTest : ScenarioTestBase() {

    init {
        context("Wandering Mind ETB — look at 6, may reveal a noncreature nonland card") {

            test("the noncreature, nonland card can be revealed and put into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wandering Mind")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Top six of the library: one noncreature/nonland (Lightning Bolt) plus five
                    // creatures that don't qualify.
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wandering Mind").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("presents a selection over the top six cards") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.size shouldBe 1
                }
                val bolt = (decision as SelectCardsDecision).options.first()

                game.selectCards(listOf(bolt)).error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt (the noncreature, nonland card) is now in hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                }
                withClue("Wandering Mind is on the battlefield as a 2/1 flyer") {
                    val mind = game.findPermanent("Wandering Mind")!!
                    game.state.projectedState.getPower(mind) shouldBe 2
                    game.state.projectedState.getToughness(mind) shouldBe 1
                    game.state.projectedState.hasKeyword(mind, Keyword.FLYING) shouldBe true
                }
            }

            test("declining the reveal puts all six looked-at cards on the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wandering Mind")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)

                game.castSpell(1, "Wandering Mind").error shouldBe null
                game.resolveStack()
                game.skipSelection() // decline the optional reveal
                game.resolveStack()

                withClue("Lightning Bolt is NOT in hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe false
                }
                withClue("all six looked-at cards return to the library") {
                    game.librarySize(1) shouldBe librarySizeBefore
                }
            }
        }
    }
}
