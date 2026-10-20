package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eureka (LEG #185).
 *
 * "Starting with you, each player may put a permanent card from their hand onto the battlefield.
 *  Repeat this process until no one puts a card onto the battlefield."
 *
 * Pins the loop's shape: the caster chooses first each round, a player who passes in one round may
 * still put a card down in a later one, the process stops only after a full round of passes, and
 * non-permanent cards can't be put onto the battlefield.
 */
class EurekaScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Eureka")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInHand(1, "Savannah Lions")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Eureka") {

            test("players alternate, starting with the caster, until a full round of passes") {
                val game = board()
                game.castSpell(1, "Eureka").error shouldBe null
                game.resolveStack()

                // Round 1 — the caster chooses first and puts Grizzly Bears down.
                withClue("the caster chooses first") {
                    game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null

                // The opponent passes this round.
                withClue("then the opponent chooses") {
                    game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player2Id
                }
                game.skipSelection().error shouldBe null

                // Round 2 — someone put a card down, so it repeats: Savannah Lions, and now the
                // opponent, who passed last round, puts Hill Giant down.
                withClue("the process repeats because a card was put onto the battlefield") {
                    game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                }
                game.selectCards(game.findCardsInHand(1, "Savannah Lions")).error shouldBe null
                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player2Id
                game.selectCards(game.findCardsInHand(2, "Hill Giant")).error shouldBe null

                // Round 3 — the caster has only Lightning Bolt left, which isn't a permanent card,
                // and the opponent's hand is empty: nobody puts anything down, so it ends.
                while (game.getPendingDecision() != null) {
                    game.skipSelection().error shouldBe null
                }

                withClue("everything chosen is on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Lightning Bolt stays in hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                }
                withClue("Eureka has finished resolving") {
                    game.state.stack.isEmpty() shouldBe true
                    game.isInGraveyard(1, "Eureka") shouldBe true
                }
            }

            test("it ends after the first round when everyone passes") {
                val game = board()
                game.castSpell(1, "Eureka").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                game.skipSelection().error shouldBe null
                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player2Id
                game.skipSelection().error shouldBe null

                withClue("no further round is offered") {
                    game.getPendingDecision() shouldBe null
                }
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Eureka") shouldBe true
            }

            test("cast on an opponent's turn, the caster still chooses first") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Anticipation")
                    .withCardInHand(1, "Eureka")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Eureka").error shouldBe null
                game.resolveStack()

                withClue("starting with you means the caster, not the active player") {
                    game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                withClue("then the next player in turn order") {
                    game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player2Id
                }
                game.selectCards(game.findCardsInHand(2, "Hill Giant")).error shouldBe null
                while (game.getPendingDecision() != null) {
                    game.skipSelection().error shouldBe null
                }

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
