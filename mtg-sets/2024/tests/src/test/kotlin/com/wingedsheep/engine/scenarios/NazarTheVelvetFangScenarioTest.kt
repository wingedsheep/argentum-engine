package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nazar, the Velvet Fang (J25 #43):
 *   Whenever you gain life, put a feeding counter on Nazar.
 *   Whenever Nazar attacks, you may remove three feeding counters from it. If you do, you draw
 *   three cards and you lose 3 life.
 *
 * Each life-gaining event adds one feeding counter regardless of the amount. The attack payoff is
 * a cost paid in full or not at all: with three counters it is offered and draws three for 3 life;
 * with two there is no prompt and nothing happens.
 */
class NazarTheVelvetFangScenarioTest : ScenarioTestBase() {

    private fun setup(feeding: Int = 0, phase: Phase = Phase.COMBAT, step: Step = Step.DECLARE_ATTACKERS): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Nazar, the Velvet Fang")
            .withCardInHand(1, "Chaplain's Blessing")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .inPhase(phase, step)
            .build()
        if (feeding > 0) {
            val nazar = game.findPermanent("Nazar, the Velvet Fang")!!
            game.state = game.state.updateEntity(nazar) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.FEEDING, feeding))
            }
        }
        return game
    }

    private fun TestGame.feeding(): Int =
        state.getEntity(findPermanent("Nazar, the Velvet Fang")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.FEEDING) ?: 0

    init {
        context("Nazar, the Velvet Fang") {

            test("gaining life puts one feeding counter on Nazar, whatever the amount") {
                val game = setup(phase = Phase.PRECOMBAT_MAIN, step = Step.PRECOMBAT_MAIN)

                game.castSpell(1, "Chaplain's Blessing").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 25
                game.feeding() shouldBe 1
            }

            test("with three feeding counters, removing them draws three cards and loses 3 life") {
                val game = setup(feeding = 3)
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Nazar, the Velvet Fang" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger offers the counter removal") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.feeding() shouldBe 0
                game.handSize(1) shouldBe handBefore + 3
                game.getLifeTotal(1) shouldBe 17
            }

            test("declining keeps the counters and draws nothing") {
                val game = setup(feeding = 4)
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Nazar, the Velvet Fang" to 2)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.feeding() shouldBe 4
                game.handSize(1) shouldBe handBefore
                game.getLifeTotal(1) shouldBe 20
            }

            test("with only two feeding counters there is no prompt and nothing happens") {
                val game = setup(feeding = 2)
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Nazar, the Velvet Fang" to 2)).error shouldBe null
                game.resolveStack()

                (game.state.pendingDecision is YesNoDecision) shouldBe false
                game.feeding() shouldBe 2
                game.handSize(1) shouldBe handBefore
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
