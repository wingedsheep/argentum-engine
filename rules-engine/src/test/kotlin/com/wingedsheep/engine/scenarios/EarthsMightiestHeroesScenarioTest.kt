package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Earth's Mightiest Heroes (MSH #165) — {4}{G}{G} Sorcery.
 *
 *   Teamwork 5
 *   Reveal the top eight cards of your library. You may put a creature card from among them onto
 *   the battlefield. If this spell was cast using teamwork, put any number of creature cards from
 *   among them onto the battlefield instead. Put the rest into your graveyard.
 *
 * The library is seeded with exactly eight cards — three creatures and five Forests — so the
 * reveal takes the whole library and the difference between the branches is visible in one number:
 * the selection's maximum is 1 without teamwork and 3 (every eligible creature) with it. Craw Wurm
 * (6/4) is on the battlefield only to pay teamwork 5 by itself.
 */
class EarthsMightiestHeroesScenarioTest : ScenarioTestBase() {

    init {
        context("Earth's Mightiest Heroes") {

            fun board() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Earth's Mightiest Heroes")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Wall of Swords")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("cast without teamwork puts at most one creature card onto the battlefield") {
                val game = board()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").first()
                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()

                game.castSpell(1, "Earth's Mightiest Heroes").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("\"you may put a creature card\" is a choose-up-to-one") {
                    decision.maxSelections shouldBe 1
                    decision.minSelections shouldBe 0
                }

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("the other two creatures were not chosen, so they join the rest in the graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Wall of Swords") shouldBe true
                }
                withClue("all eight revealed cards left the library") {
                    game.state.getLibrary(game.player1Id).shouldBeEmpty()
                }
                withClue("no teamwork cost was declared, so nothing tapped") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork puts any number of creature cards onto the battlefield") {
                val game = board()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").first()
                val giant = game.findCardsInLibrary(1, "Hill Giant").first()
                val wall = game.findCardsInLibrary(1, "Wall of Swords").first()
                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()

                // Teamwork 5 — the 6/4 Craw Wurm clears the threshold on its own.
                game.castSpellWithTeamwork(1, "Earth's Mightiest Heroes", "Craw Wurm")
                    .error shouldBe null
                game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("\"any number\" reaches every eligible creature among the eight revealed") {
                    decision.maxSelections shouldBe 3
                    decision.minSelections shouldBe 0
                }

                game.selectCards(listOf(bears, giant, wall)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Wall of Swords") shouldBe true
                withClue("the five noncreature cards are the rest, and go to the graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                    game.state.getLibrary(game.player1Id).shouldBeEmpty()
                }
            }

            // `minSelections == 0` is asserted on both branches above but never exercised. "You
            // *may* put" means declining is legal, and then every revealed card is "the rest".
            test("selecting nothing is legal and sends all eight revealed cards to the graveyard") {
                val game = board()

                game.castSpell(1, "Earth's Mightiest Heroes").error shouldBe null
                game.resolveStack()

                game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                    .minSelections shouldBe 0

                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("nothing was chosen, so no creature reaches the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Wall of Swords") shouldBe false
                }
                withClue("all eight revealed cards are 'the rest'") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Wall of Swords") shouldBe true
                    game.findCardsInGraveyard(1, "Forest") shouldHaveSize 5
                    game.state.getLibrary(game.player1Id).shouldBeEmpty()
                }
            }

            // "Reveal the top eight cards" takes as many as are there — a three-card library is
            // not an error and does not stall the resolution.
            test("a library of fewer than eight cards reveals as many as it has") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Earth's Mightiest Heroes")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").first()

                game.castSpell(1, "Earth's Mightiest Heroes").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only one of the three revealed cards is a creature card") {
                    decision.maxSelections shouldBe 1
                }

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("the two Forests are the rest, and the library is emptied, not underflowed") {
                    game.findCardsInGraveyard(1, "Forest") shouldHaveSize 2
                    game.state.getLibrary(game.player1Id).shouldBeEmpty()
                }
            }
        }
    }
}
