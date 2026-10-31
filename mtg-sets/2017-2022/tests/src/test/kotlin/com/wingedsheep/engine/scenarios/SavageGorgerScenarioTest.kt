package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Savage Gorger (M20 #291) — {1}{B}{B} 1/1 flying Vampire.
 *
 * "At the beginning of your end step, if an opponent lost life this turn, put a +1/+1 counter on
 *  this creature."
 *
 * Covers both sides of the intervening "if": an opponent's life loss this turn grows it; no loss,
 * no counter.
 */
class SavageGorgerScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Savage Gorger") {

            test("gets a +1/+1 counter at your end step when an opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Savage Gorger")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gorger = game.findPermanent("Savage Gorger")!!
                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("opponent lost life -> one counter -> 2/2") {
                    game.plusOnes(gorger) shouldBe 1
                    game.state.projectedState.getPower(gorger) shouldBe 2
                    game.state.projectedState.getToughness(gorger) shouldBe 2
                }
            }

            test("no counter when no opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Savage Gorger")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gorger = game.findPermanent("Savage Gorger")!!
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.plusOnes(gorger) shouldBe 0
                game.state.projectedState.getPower(gorger) shouldBe 1
            }
        }
    }
}
