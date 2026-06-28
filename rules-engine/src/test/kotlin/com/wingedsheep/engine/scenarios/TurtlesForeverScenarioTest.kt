package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Turtles Forever (TMT):
 * "Search your library and/or outside the game for exactly four legendary creature cards you own
 *  with different names, then reveal those cards. An opponent chooses two of them. Put the chosen
 *  cards into your hand and shuffle the rest into your library."
 *
 * Exercises the full "you assemble, they split" pipeline: a multi-zone search (library + the
 * private SIDEBOARD = "outside the game"), an exactly-four/different-names selection, an
 * opponent-chooser split, and the chosen→hand / rest→library routing. Notably, an unchosen card
 * pulled from outside the game ends up in the library, not back in the sideboard.
 */
class TurtlesForeverScenarioTest : ScenarioTestBase() {

    init {
        context("Turtles Forever assembles four legends; the opponent splits two into the caster's hand") {
            test("chosen two go to hand, the rest (incl. a sideboard pull) are shuffled into the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Turtles Forever")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Splinter, Hamato Yoshi")
                    .withCardInLibrary(1, "Leonardo, Big Brother")
                    .withCardInLibrary(1, "Donatello, Gadget Master")
                    .withCardInLibrary(1, "Shock") // nonlegendary — must NOT be searchable
                    .withCardInSideboard(1, "Michelangelo, Game Master")
                    .withCardInSideboard(1, "Raphael, Tough Turtle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Turtles Forever")
                withClue("Turtles Forever should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Decision 1: the caster searches for exactly four legendary creatures, different names.
                val search = game.getPendingDecision()
                (search is SelectCardsDecision) shouldBe true
                val searchable = (search as SelectCardsDecision).cardInfo!!
                withClue("All five legendary creatures across library + sideboard are eligible") {
                    searchable.values.count {
                        it.name in setOf(
                            "Splinter, Hamato Yoshi", "Leonardo, Big Brother", "Donatello, Gadget Master",
                            "Michelangelo, Game Master", "Raphael, Tough Turtle"
                        )
                    } shouldBe 5
                }
                withClue("The nonlegendary card is never searchable") {
                    searchable.values.any { it.name == "Shock" } shouldBe false
                }
                fun searchId(name: String) = searchable.entries.first { it.value.name == name }.key
                game.selectCards(
                    listOf(
                        "Splinter, Hamato Yoshi", "Leonardo, Big Brother",
                        "Donatello, Gadget Master", "Michelangelo, Game Master"
                    ).map { searchId(it) }
                )

                // Decision 2: an opponent chooses two of the four revealed cards.
                val split = game.getPendingDecision()
                (split is SelectCardsDecision) shouldBe true
                val pile = (split as SelectCardsDecision).cardInfo!!
                withClue("Only the four found cards form the split pile (the un-found legend is excluded)") {
                    pile.size shouldBe 4
                    pile.values.any { it.name == "Raphael, Tough Turtle" } shouldBe false
                }
                fun splitId(name: String) = pile.entries.first { it.value.name == name }.key
                game.selectCards(listOf(splitId("Splinter, Hamato Yoshi"), splitId("Leonardo, Big Brother")))
                game.resolveStack()

                withClue("The opponent's two chosen cards are put into the caster's hand") {
                    game.isInHand(1, "Splinter, Hamato Yoshi") shouldBe true
                    game.isInHand(1, "Leonardo, Big Brother") shouldBe true
                }

                val libraryNames = game.state.getLibrary(game.player1Id).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                }
                withClue("The unchosen found cards are shuffled into the library, not put in hand") {
                    game.isInHand(1, "Donatello, Gadget Master") shouldBe false
                    game.isInHand(1, "Michelangelo, Game Master") shouldBe false
                    libraryNames.contains("Donatello, Gadget Master") shouldBe true
                    libraryNames.contains("Michelangelo, Game Master") shouldBe true
                }
                withClue("The unchosen card pulled from outside the game lands in the library, not the sideboard") {
                    game.isInSideboard(1, "Michelangelo, Game Master") shouldBe false
                }
                withClue("A legendary creature that was never found stays outside the game") {
                    game.isInSideboard(1, "Raphael, Tough Turtle") shouldBe true
                }
            }

            // "exactly four" is a ceiling, not a requirement: search finds as many as exist. With
            // only three different-named legends across both zones, ChooseExactly(4) + OnePerCardName
            // clamps to three (the opponent then splits two into hand, one back into the library).
            test("fewer than four eligible legends clamps the search down to what exists") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Turtles Forever")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Splinter, Hamato Yoshi")
                    .withCardInLibrary(1, "Leonardo, Big Brother")
                    .withCardInSideboard(1, "Michelangelo, Game Master")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Turtles Forever")
                withClue("Turtles Forever should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Decision 1: only three legends exist, so the "exactly four" search clamps to three.
                val search = game.getPendingDecision()
                (search is SelectCardsDecision) shouldBe true
                val searchable = (search as SelectCardsDecision).cardInfo!!
                withClue("All three available legends are offered — the count clamps below four") {
                    searchable.size shouldBe 3
                }
                fun searchId(name: String) = searchable.entries.first { it.value.name == name }.key
                game.selectCards(
                    listOf("Splinter, Hamato Yoshi", "Leonardo, Big Brother", "Michelangelo, Game Master")
                        .map { searchId(it) }
                )

                // Decision 2: the opponent splits the three found cards — two to hand, one to library.
                val split = game.getPendingDecision()
                (split is SelectCardsDecision) shouldBe true
                val pile = (split as SelectCardsDecision).cardInfo!!
                pile.size shouldBe 3
                fun splitId(name: String) = pile.entries.first { it.value.name == name }.key
                game.selectCards(listOf(splitId("Splinter, Hamato Yoshi"), splitId("Leonardo, Big Brother")))
                game.resolveStack()

                withClue("The two chosen go to hand") {
                    game.isInHand(1, "Splinter, Hamato Yoshi") shouldBe true
                    game.isInHand(1, "Leonardo, Big Brother") shouldBe true
                }
                val libraryNames = game.state.getLibrary(game.player1Id).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                }
                withClue("The lone remainder (a sideboard pull) is shuffled into the library") {
                    game.isInHand(1, "Michelangelo, Game Master") shouldBe false
                    game.isInSideboard(1, "Michelangelo, Game Master") shouldBe false
                    libraryNames.contains("Michelangelo, Game Master") shouldBe true
                }
            }
        }
    }
}
