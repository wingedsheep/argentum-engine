package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Vizkopa Confessor (GTC #205).
 *
 * "When this creature enters, pay any amount of life. Target opponent reveals that many cards from
 *  their hand. You choose one of them and exile it."
 *
 * Three decisions by two players: the controller names the life amount on resolution, the
 * opponent picks which that-many cards to reveal, and the controller picks the one to exile.
 * Paying 0 reveals and exiles nothing; paying at least the hand size reveals the whole hand.
 */
class VizkopaConfessorScenarioTest : ScenarioTestBase() {

    private fun namesIn(game: TestGame, ids: List<EntityId>) =
        ids.map { game.state.getEntity(it)?.get<CardComponent>()?.name }

    private fun confessorGame() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Vizkopa Confessor")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInHand(2, "Volcanic Hammer")
        .withCardInHand(2, "Mind Rot")
        .withCardInHand(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun castAndResolve(game: TestGame) {
        game.castSpell(1, "Vizkopa Confessor")
        game.resolveStack()
    }

    init {
        context("Vizkopa Confessor") {

            test("paying 2 life: the opponent reveals two cards, you exile one of them") {
                val game = confessorGame()
                castAndResolve(game)

                val pay = game.getPendingDecision()
                pay.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("Alice names the amount, up to her life total") {
                    pay.playerId shouldBe game.player1Id
                    pay.minValue shouldBe 0
                    pay.maxValue shouldBe 20
                }
                game.chooseNumber(2).error shouldBe null
                game.getLifeTotal(1) shouldBe 18

                val reveal = game.getPendingDecision()
                reveal.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob chooses which two cards to reveal") {
                    reveal.playerId shouldBe game.player2Id
                    reveal.minSelections shouldBe 2
                    reveal.maxSelections shouldBe 2
                }
                val revealed = reveal.options.filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name != "Hill Giant"
                }
                game.selectCards(revealed).error shouldBe null

                val pick = game.getPendingDecision()
                pick.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Alice chooses among only the two revealed cards, and must choose one") {
                    pick.playerId shouldBe game.player1Id
                    pick.minSelections shouldBe 1
                    namesIn(game, pick.options).toSet() shouldBe setOf("Volcanic Hammer", "Mind Rot")
                }
                val mindRot = pick.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Mind Rot"
                }
                game.selectCards(listOf(mindRot)).error shouldBe null
                game.resolveStack()

                namesIn(game, game.state.getExile(game.player2Id)) shouldBe listOf("Mind Rot")
                game.handSize(2) shouldBe 2
            }

            test("paying 0 life reveals and exiles nothing") {
                val game = confessorGame()
                castAndResolve(game)

                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(0).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.getLifeTotal(1) shouldBe 20
                game.handSize(2) shouldBe 3
                game.state.getExile(game.player2Id).size shouldBe 0
            }

            test("paying more than the hand size reveals the whole hand") {
                val game = confessorGame()
                castAndResolve(game)

                game.chooseNumber(5).error shouldBe null
                game.getLifeTotal(1) shouldBe 15

                val pick = game.getPendingDecision()
                pick.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob has no choice to make; Alice picks from all three cards") {
                    pick.playerId shouldBe game.player1Id
                    pick.options.size shouldBe 3
                }
                val giant = pick.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                }
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                namesIn(game, game.state.getExile(game.player2Id)) shouldBe listOf("Hill Giant")
                game.handSize(2) shouldBe 2
            }
        }
    }
}
