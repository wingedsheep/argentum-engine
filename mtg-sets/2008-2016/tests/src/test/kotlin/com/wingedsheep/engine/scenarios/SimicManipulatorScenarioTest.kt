package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.gtc.cards.SimicManipulator
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Simic Manipulator (GTC #50) — {1}{U}{U} 0/1 Mutant Wizard.
 *
 *   Evolve
 *   {T}, Remove one or more +1/+1 counters from this creature: Gain control of target creature with
 *   power less than or equal to the number of +1/+1 counters removed this way.
 *
 * The cost-linked amount is the point: the counters removed from the Manipulator itself are the X
 * that caps the target's power, and "one or more" forbids X = 0.
 */
class SimicManipulatorScenarioTest : ScenarioTestBase() {

    private val abilityId = SimicManipulator.activatedAbilities.first().id

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup(counters: Int): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Simic Manipulator")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Ornithopter")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val manipulator = game.findPermanent("Simic Manipulator")!!
        game.state = game.state.updateEntity(manipulator) {
            it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, counters))
        }
        return game
    }

    private fun TestGame.activate(target: EntityId, x: Int) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Simic Manipulator")!!,
            abilityId = abilityId,
            targets = listOf(ChosenTarget.Permanent(target)),
            xValue = x,
        )
    )

    init {
        context("Simic Manipulator") {

            test("removing two counters steals a creature with power 2 for good") {
                val game = setup(counters = 3)
                val manipulator = game.findPermanent("Simic Manipulator")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.activate(bears, x = 2)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("exactly X counters come off as the cost") { game.plusOnes(manipulator) shouldBe 1 }
                game.resolveStack()

                withClue("Alice now controls the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
            }

            test("a creature with power greater than the counters removed can't be targeted") {
                val game = setup(counters = 3)
                val giant = game.findPermanent("Hill Giant")!!

                val result = game.activate(giant, x = 2)
                withClue("a 3-power target is illegal for X = 2") { result.error shouldNotBe null }
                game.plusOnes(game.findPermanent("Simic Manipulator")!!) shouldBe 3
                game.state.projectedState.getController(giant) shouldBe game.player2Id
            }

            test("removing zero counters is not allowed, even against a 0-power target") {
                val game = setup(counters = 1)
                val ornithopter = game.findPermanent("Ornithopter")!!

                // Ornithopter's power 0 is within an X = 0 cap, so only "one or more" can reject this.
                val result = game.activate(ornithopter, x = 0)
                withClue("X = 0 is rejected — the cost says one or more") { result.error shouldNotBe null }
                game.plusOnes(game.findPermanent("Simic Manipulator")!!) shouldBe 1
                game.state.projectedState.getController(ornithopter) shouldBe game.player2Id
            }
        }
    }
}
