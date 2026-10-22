package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Search Party Captain — "This spell costs {1} less to cast for each creature you attacked with
 * this turn. When this creature enters, draw a card."
 *
 * Exercises `CostReductionSource.CreaturesThatAttackedThisTurn(yoursOnly = true)`: the count is the
 * caster's own attack history, so a dead attacker still counts and an opponent's attackers don't.
 */
class SearchPartyCaptainScenarioTest : ScenarioTestBase() {

    private fun genericCost(game: TestGame, playerNumber: Int): Int =
        CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator).calculateEffectiveCost(
            game.state,
            cardRegistry.requireCard("Search Party Captain"),
            if (playerNumber == 1) game.player1Id else game.player2Id,
        ).genericAmount

    init {
        context("Search Party Captain cost reduction") {

            test("no attack → full {3}{W}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Search Party Captain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                genericCost(game, 1) shouldBe 3
            }

            test("an attacker that died still counts, and the spell resolves for {1}{W} and draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Search Party Captain")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Grizzly Bears died blocked by Hill Giant") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("both attackers discount the Captain, dead or not") {
                    genericCost(game, 1) shouldBe 1
                }

                val handBefore = game.handSize(1)
                game.castSpell(1, "Search Party Captain").error shouldBe null
                game.resolveStack()

                game.findPermanent("Search Party Captain") shouldNotBe null
                withClue("cast removes the Captain from hand, the ETB draws one") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("an opponent's attackers don't discount it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Search Party Captain")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                genericCost(game, 2) shouldBe 3
            }

            test("the reduction never eats the {W} pip") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Search Party Captain")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Hill Giant" to 2, "Savannah Lions" to 2, "Centaur Courser" to 2)
                ).error shouldBe null

                val cost = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator)
                    .calculateEffectiveCost(game.state, cardRegistry.requireCard("Search Party Captain"), game.player1Id)
                cost.genericAmount shouldBe 0
                cost.colorCount[Color.WHITE] shouldBe 1
            }
        }
    }
}
