package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kothophed, Soul Hoarder (ORI #104).
 *
 * "Whenever a permanent owned by another player is put into a graveyard from the battlefield,
 *  you draw a card and you lose 1 life."
 *
 * Pins the owner-not-controller reading (ruling 2015-06-22): the trigger filter is
 * `Not(OwnedByYou)`, evaluated against the leaving permanent's owner.
 */
class KothophedSoulHoarderScenarioTest : ScenarioTestBase() {

    init {
        context("Kothophed, Soul Hoarder") {

            test("an opponent-owned permanent dying draws a card and loses 1 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kothophed, Soul Hoarder")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)
                val cast = game.castSpell(1, "Murder", bears)
                withClue("Murder should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears died") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("Murder left the hand, Kothophed drew one") { game.handSize(1) shouldBe handBefore }
                withClue("Kothophed's controller lost 1 life") { game.getLifeTotal(1) shouldBe 19 }
                withClue("the opponent is unaffected") { game.getLifeTotal(2) shouldBe 20 }
            }

            test("a permanent you own dying does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kothophed, Soul Hoarder")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)
                game.castSpell(1, "Murder", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears died") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("no card drawn") { game.handSize(1) shouldBe handBefore - 1 }
                withClue("no life lost") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("an opponent-owned permanent you control still triggers") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kothophed, Soul Hoarder")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Act of Treason")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Act of Treason", bears).error shouldBe null
                game.resolveStack()
                withClue("Act of Treason gave Player control of the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }

                val handBefore = game.handSize(1)
                game.castSpell(1, "Murder", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears died") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("Murder left the hand, Kothophed drew one") { game.handSize(1) shouldBe handBefore }
                withClue("Kothophed's controller lost 1 life") { game.getLifeTotal(1) shouldBe 19 }
            }
        }
    }
}
