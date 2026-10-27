package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Extrapolate the Impossible {1}{B} — Sorcery:
 * "You may reveal exactly two cards you own with different names from outside the game. An
 *  opponent chooses one of them. You put that card into your hand."
 *
 * "Outside the game" is the private sideboard. Pins: the caster picks two differently named
 * cards, the *opponent* picks which one goes to the caster's hand, the other stays outside the
 * game; declining does nothing; a sideboard without two different names offers no choice at all;
 * and a same-name pair can't satisfy "exactly two with different names".
 */
class ExtrapolateTheImpossibleScenarioTest : ScenarioTestBase() {

    init {
        context("revealing two differently named cards") {
            test("the opponent chooses which revealed card goes to the caster's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Extrapolate the Impossible")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInSideboard(1, "Swelter")
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withCardInSideboard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Extrapolate the Impossible").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                val reveal = game.getPendingDecision()
                reveal.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The caster picks exactly two, one per name") {
                    reveal.playerId shouldBe game.player1Id
                    reveal.minSelections shouldBe 2
                    reveal.maxSelections shouldBe 2
                    reveal.onePerCardName shouldBe true
                }
                val byName = reveal.cardInfo!!.entries.associate { it.value.name to it.key }
                game.selectCards(listOf(byName.getValue("Swelter"), byName.getValue("Grizzly Bears")))

                val pick = game.getPendingDecision()
                pick.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The opponent chooses, between exactly the two revealed cards") {
                    pick.playerId shouldBe game.player2Id
                    pick.options.toSet() shouldBe setOf(byName.getValue("Swelter"), byName.getValue("Grizzly Bears"))
                    pick.minSelections shouldBe 1
                    pick.maxSelections shouldBe 1
                }
                game.selectCards(listOf(byName.getValue("Grizzly Bears")))

                withClue("The opponent's choice goes to the caster's hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInSideboard(1, "Grizzly Bears") shouldBe false
                }
                withClue("The other revealed card and the unrevealed one stay outside the game") {
                    game.isInSideboard(1, "Swelter") shouldBe true
                    game.isInSideboard(1, "Lightning Bolt") shouldBe true
                    game.isInHand(1, "Swelter") shouldBe false
                }
                withClue("Extrapolate the Impossible goes to the graveyard") {
                    game.isInGraveyard(1, "Extrapolate the Impossible") shouldBe true
                }
            }

            test("a same-name pair can't be revealed, so nothing is put into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Extrapolate the Impossible")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withCardInSideboard(1, "Swelter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Extrapolate the Impossible").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)

                val reveal = game.getPendingDecision()
                reveal.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = reveal.cardInfo!!.entries.filter { it.value.name == "Grizzly Bears" }.map { it.key }
                bears.size shouldBe 2
                // A client that ignored the name restriction: the server keeps only one Bears.
                game.selectCards(bears)

                withClue("Only one card could be revealed — no opponent choice, nothing to hand") {
                    game.hasPendingDecision() shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                    game.sideboardSize(1) shouldBe 3
                }
            }
        }

        context("declining or being unable to reveal") {
            test("declining reveals nothing and fetches nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Extrapolate the Impossible")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInSideboard(1, "Swelter")
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Extrapolate the Impossible").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)

                game.hasPendingDecision() shouldBe false
                game.sideboardSize(1) shouldBe 2
                game.isInHand(1, "Swelter") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe false
            }

            test("a sideboard without two different names offers only no") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Extrapolate the Impossible")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Extrapolate the Impossible").error shouldBe null
                game.resolveStack()

                // Still asked, because the sideboard is hidden and skipping would reveal it (#3340),
                // but two copies of one card can't be "two cards with different names" (CR 608.2d).
                withClue("only no is possible") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>().yesAvailable shouldBe false
                }
                game.answerYesNo(false).error shouldBe null
                game.hasPendingDecision() shouldBe false
                game.sideboardSize(1) shouldBe 2
                game.isInHand(1, "Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Extrapolate the Impossible") shouldBe true
            }
        }
    }
}
