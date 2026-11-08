package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hurska Sweet-Tooth (J25 #53):
 *   Whenever Hurska attacks, create a Food token.
 *   Whenever you gain life, you may pay {G/W}. When you do, target creature gets +X/+X until end
 *   of turn, where X is the amount of life you gained.
 *
 * X is the triggering life-gain event's amount, read inside the *reflexive* ability — which only
 * works because that ability inherits the resolving trigger's record. Life gained earlier in the
 * turn must not count.
 */
class HurskaSweetToothScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (state.pendingDecision is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun setup(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Hurska Sweet-Tooth")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Natural Spring")
            .withLandsOnBattlefield(1, "Forest", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        // Life already gained this turn — X must ignore it and read only the triggering gain.
        game.state = game.state.updateEntity(game.player1Id) {
            it.withComponent(LifeGainedAmountThisTurnComponent(5))
        }
        return game
    }

    init {
        context("Hurska Sweet-Tooth") {

            test("paying {G/W} gives the chosen creature +X/+X where X is the life just gained") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(1, "Natural Spring", 1).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.payIfAsked()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gets +8/+8 from the 8-life gain, not the turn's 13 total") {
                    game.state.projectedState.getPower(bears) shouldBe 10
                    game.state.projectedState.getToughness(bears) shouldBe 10
                }
            }

            test("declining to pay pumps nothing") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(1, "Natural Spring", 1).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }
    }
}
