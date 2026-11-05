package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wretched Camel (HOU #82) — {1}{B} Creature — Zombie Camel 2/1.
 *
 *   When this creature dies, if you control a Desert or there is a Desert card in your graveyard,
 *   target player discards a card.
 */
class WretchedCamelScenarioTest : ScenarioTestBase() {

    private val name = "Wretched Camel"
    private val desert = "Desert of the Fervent"

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, name)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInHand(2, "Grizzly Bears")

        fun boltCamel(game: TestGame) {
            val camel = game.findPermanent(name)!!
            game.castSpell(1, "Lightning Bolt", camel).error shouldBe null
            game.resolveStack()
        }

        fun opponentDiscards(game: TestGame) {
            boltCamel(game)
            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(listOf(game.findCardsInHand(2, "Grizzly Bears").first())).error shouldBe null
                game.resolveStack()
            }
            withClue("Player2 discarded their only card") {
                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        test("a Desert you control turns the dies trigger on") {
            opponentDiscards(board().withCardOnBattlefield(1, desert).build())
        }

        test("a Desert card in your graveyard turns the dies trigger on") {
            opponentDiscards(board().withCardInGraveyard(1, desert).build())
        }

        test("with no Desert of your own the dies trigger does nothing") {
            val game = board()
                .withCardInGraveyard(1, "Swamp")
                .withCardOnBattlefield(2, desert)
                .build()
            boltCamel(game)
            withClue("no target prompt: the intervening-if is false (an opponent's Desert doesn't count)") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
            }
            game.isInGraveyard(1, name) shouldBe true
            game.handSize(2) shouldBe 1
        }
    }
}
