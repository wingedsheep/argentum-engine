package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pact of Negation: "Counter target spell. At the beginning of your next upkeep, pay {3}{U}{U}.
 * If you don't, you lose the game."
 *
 * The delayed trigger must skip the opponent's upkeep and land on the caster's own next upkeep.
 */
class PactOfNegationScenarioTest : ScenarioTestBase() {

    /** Player 1 bolts player 2, then counters their own Bolt with Pact; round to player 1's next upkeep. */
    private fun pactAndRoundTheTable(): TestGame {
        val game = scenario()
            .withPlayers("Pactmaker", "Opponent")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInHand(1, "Pact of Negation")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
        val bolt = game.state.stack.last()
        val pact = game.findCardsInHand(1, "Pact of Negation").single()
        game.execute(CastSpell(game.player1Id, pact, listOf(ChosenTarget.Spell(bolt)))).error shouldBe null
        game.resolveStack()

        withClue("the Bolt was countered") {
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }

        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.activePlayerId shouldBe game.player2Id
        withClue("nothing happens on the opponent's upkeep") {
            game.state.stack.isEmpty() shouldBe true
            game.hasPendingDecision() shouldBe false
        }

        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.activePlayerId shouldBe game.player1Id
        game.resolveStack()
        return game
    }

    init {
        test("not paying at your next upkeep loses the game") {
            val game = pactAndRoundTheTable()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.getPendingDecision()!!.playerId shouldBe game.player1Id
            game.answerYesNo(false)
            game.resolveStack()

            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player2Id
        }

        test("paying {3}{U}{U} at your next upkeep keeps you in the game") {
            val game = pactAndRoundTheTable()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.gameOver shouldBe false
        }
    }
}
