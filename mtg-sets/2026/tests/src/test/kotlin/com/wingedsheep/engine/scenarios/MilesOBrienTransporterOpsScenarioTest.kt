package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Miles O'Brien, Transporter Ops: the enters trigger returns up to one other creature or artifact
 * you control to its owner's hand, and at the beginning of each end step you draw a card if a
 * permanent was returned to *your* hand from the battlefield this turn — whoever returned it.
 */
class MilesOBrienTransporterOpsScenarioTest : ScenarioTestBase() {

    init {
        context("Miles O'Brien, Transporter Ops") {
            test("bouncing your own creature with the enters trigger draws a card at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Miles O'Brien, Transporter Ops")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Miles O'Brien, Transporter Ops").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                }
                game.resolveStack()

                withClue("the Bears went back to hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
                game.handSize(1) shouldBe 1

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Miles draws because a permanent was returned to your hand") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("an opponent bouncing your permanent on their turn still counts") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Miles O'Brien, Transporter Ops")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Unsummon")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Unsummon", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 1

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Miles triggers in each end step, and the Bears went to your hand") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("a permanent returned to an opponent's hand doesn't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Miles O'Brien, Transporter Ops")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 0

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Bears went to the opponent's hand, not yours") { game.handSize(1) shouldBe 0 }
            }

            test("no draw when nothing was returned this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Miles O'Brien, Transporter Ops")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.handSize(1) shouldBe 0
            }
        }
    }
}
