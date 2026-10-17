package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Even the Score — "This spell costs {U}{U}{U} less to cast if an opponent has drawn four or more
 * cards this turn. Draw X cards." With two Islands only, X=2 is castable exactly when the
 * reduction applies; the gate reads the opponent's draws, never the caster's.
 */
class EvenTheScoreScenarioTest : ScenarioTestBase() {

    private fun board(yourDraws: Int, opponentDraws: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Even the Score")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardsDrawnThisTurn(1, yourDraws)
        .withCardsDrawnThisTurn(2, opponentDraws)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun castWithX(game: TestGame, x: Int) = game.execute(
        CastSpell(
            playerId = game.player1Id,
            cardId = game.findCardsInHand(1, "Even the Score").single(),
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    init {
        test("opponent drew four cards: costs only {X} and draws X") {
            val game = board(yourDraws = 0, opponentDraws = 4)

            castWithX(game, 2).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            game.isInGraveyard(1, "Even the Score") shouldBe true
        }

        test("opponent drew only three cards: full {U}{U}{U} is still owed") {
            val game = board(yourDraws = 0, opponentDraws = 3)

            castWithX(game, 2).error shouldNotBe null
            game.handSize(1) shouldBe 1
        }

        test("your own draws do not count toward the reduction") {
            val game = board(yourDraws = 5, opponentDraws = 0)

            castWithX(game, 2).error shouldNotBe null
            game.handSize(1) shouldBe 1
        }
    }
}
