package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mysterious Egg (IKO #3) — {1} Creature — Egg 0/2.
 *
 *   Whenever this creature mutates, put a +1/+1 counter on it.
 *
 * The Egg has no mutate cost of its own, so it only mutates when another mutating creature spell
 * targets it. Porcuparrot (3/4, Mutate {2}{R}, no mutate trigger) mutates onto it: placed over, the
 * merged permanent is a 3/4 Porcuparrot that keeps the Egg's trigger; placed under, it stays a 0/2 Egg.
 * Either way the trigger fires and the merged permanent gets a +1/+1 counter.
 */
class MysteriousEggScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.mutateOnto(host: EntityId, over: Boolean) {
        val offer = getLegalActions(1).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
        }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(host)))).error shouldBe null
        resolveStack()
        answerYesNo(over)
        resolveStack()
    }

    private fun board() = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Mysterious Egg")
        .withCardInHand(1, "Porcuparrot")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Mysterious Egg") {

            test("mutating over the Egg puts a +1/+1 counter on the merged permanent") {
                val game = board()
                val egg = game.findPermanent("Mysterious Egg")!!
                game.mutateOnto(egg, over = true)

                withClue("the Egg's permanent is now Porcuparrot") {
                    game.findPermanent("Porcuparrot") shouldBe egg
                }
                game.plusOneCounters(egg) shouldBe 1
                game.state.projectedState.getPower(egg) shouldBe 4
                game.state.projectedState.getToughness(egg) shouldBe 5
            }

            test("mutating under the Egg puts a +1/+1 counter on the merged permanent") {
                val game = board()
                val egg = game.findPermanent("Mysterious Egg")!!
                game.mutateOnto(egg, over = false)

                withClue("the Egg stays on top") {
                    game.findPermanent("Porcuparrot") shouldBe null
                }
                game.plusOneCounters(egg) shouldBe 1
                game.state.projectedState.getPower(egg) shouldBe 1
                game.state.projectedState.getToughness(egg) shouldBe 3
            }
        }
    }
}
