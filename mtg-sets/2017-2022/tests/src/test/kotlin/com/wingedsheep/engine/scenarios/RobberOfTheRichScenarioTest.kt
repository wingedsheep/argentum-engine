package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Robber of the Rich (ELD #138) — "Whenever this creature attacks, if defending player has more
 * cards in hand than you, exile the top card of their library. During any turn you attacked with a
 * Rogue, you may cast that card and you may spend mana as though it were mana of any color to cast
 * that spell."
 *
 * Pins the rulings: the Rogue you attacked with needn't still be on the battlefield, the permission
 * is closed on a turn you didn't attack with a Rogue, and the intervening-if compares hand sizes.
 */
class RobberOfTheRichScenarioTest : ScenarioTestBase() {

    private fun board(opponentHand: Int = 2, blocker: Boolean = false): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Robber of the Rich")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Grizzly Bears")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (opponentHand > 0) builder = builder.withCardsInHand(2, "Forest", opponentHand)
        if (blocker) builder = builder.withCardOnBattlefield(2, "Hill Giant")
        return builder.build()
    }

    private fun TestGame.attackWithRobber() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Robber of the Rich" to 2)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.castBears() =
        getLegalActions(1).singleOrNull { it.description.contains("Grizzly Bears") }

    init {
        context("Robber of the Rich") {

            test("exiles the top card and it can be cast with mana of any color the turn it attacked") {
                val game = board()
                game.attackWithRobber()
                withClue("the opponent's top card is exiled") { game.isInExile(2, "Grizzly Bears") shouldBe true }
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                val cast = game.castBears()
                withClue("two Mountains pay {1}{G}") { cast?.isAffordable shouldBe true }
                game.execute(cast!!.action).error shouldBe null
                game.resolveStack()
                withClue("the Bears resolve under the Robber's controller") {
                    game.findPermanent("Grizzly Bears")
                        ?.let { game.state.projectedState.getController(it) } shouldBe game.player1Id
                }
            }

            test("the Rogue that attacked needn't survive combat") {
                val game = board(blocker = true)
                game.attackWithRobber()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Robber of the Rich"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("Robber died in combat") { game.isInGraveyard(1, "Robber of the Rich") shouldBe true }
                withClue("you still attacked with a Rogue this turn") { game.castBears()?.isAffordable shouldBe true }
            }

            test("the exiled card can't be cast on a turn you didn't attack with a Rogue") {
                val game = board()
                game.attackWithRobber()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("it is the Robber's controller's next turn") { game.state.activePlayerId shouldBe game.player1Id }
                withClue("the card is still exiled") { game.isInExile(2, "Grizzly Bears") shouldBe true }
                withClue("no Rogue has attacked yet this turn") { game.castBears() shouldBe null }
            }

            test("nothing is exiled unless defending player has more cards in hand") {
                val game = board(opponentHand = 0)
                game.attackWithRobber()
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }
    }
}
