package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tanazir Quandrix (STX #240) — "Whenever Tanazir Quandrix attacks, you may have the base power and
 * toughness of other creatures you control become equal to Tanazir Quandrix's power and toughness
 * until end of turn."
 *
 * The rulings make this a composition worth proving: the value is Tanazir's *actual* (projected)
 * power and toughness, not its printed 4/4, Tanazir itself is untouched, and the "you may" can be
 * declined.
 */
class TanazirQuandrixScenarioTest : ScenarioTestBase() {

    private fun pumpedTanazirAttacks(accept: Boolean): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Tanazir Quandrix")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Giant Growth")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val tanazir = game.findPermanent("Tanazir Quandrix")!!
        game.castSpell(1, "Giant Growth", tanazir).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        game.state.projectedState.getPower(tanazir) shouldBe 7

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Tanazir Quandrix" to 2)).error shouldBe null
        game.resolveStack()
        if (game.hasPendingDecision()) game.answerYesNo(accept)
        game.resolveStack()
        return game
    }

    init {
        test("other creatures get base power and toughness equal to Tanazir's actual P/T") {
            val game = pumpedTanazirAttacks(accept = true)
            val bears = game.findPermanent("Grizzly Bears")!!
            val tanazir = game.findPermanent("Tanazir Quandrix")!!

            withClue("Grizzly Bears takes Tanazir's pumped 7/7, not its printed 4/4") {
                game.state.projectedState.getPower(bears) shouldBe 7
                game.state.projectedState.getToughness(bears) shouldBe 7
            }
            withClue("Tanazir itself is not an 'other' creature and keeps its own stats") {
                game.state.projectedState.getPower(tanazir) shouldBe 7
                game.state.projectedState.getToughness(tanazir) shouldBe 7
            }
        }

        test("declining the 'you may' leaves the other creatures alone") {
            val game = pumpedTanazirAttacks(accept = false)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 2
        }
    }
}
