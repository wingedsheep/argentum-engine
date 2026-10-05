package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cruel Sadist (M15 #93) — {B} 1/1 Human Assassin.
 *
 *   {B}, {T}, Pay 1 life: Put a +1/+1 counter on this creature.
 *   {2}{B}, {T}, Remove X +1/+1 counters from this creature: It deals X damage to target creature.
 */
class CruelSadistScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Cruel Sadist") {

            test("{B}, {T}, pay 1 life puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cruel Sadist")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sadist = game.findPermanent("Cruel Sadist")!!
                val ability = cardRegistry.getCard("Cruel Sadist")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sadist, abilityId = ability.id)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("costs are paid on activation: tapped and 1 life paid") {
                    game.state.getEntity(sadist)!!.has<TappedComponent>() shouldBe true
                    game.getLifeTotal(1) shouldBe 19
                }
                game.resolveStack()

                game.plusOneCounters(sadist) shouldBe 1
                game.state.projectedState.getPower(sadist) shouldBe 2
                game.state.projectedState.getToughness(sadist) shouldBe 2
            }

            test("removing X=2 of 3 counters deals 2 damage to the target and leaves 1 counter") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cruel Sadist")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sadist = game.findPermanent("Cruel Sadist")!!
                game.state = game.state.updateEntity(sadist) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
                }
                val giant = game.findPermanent("Hill Giant")!!
                withClue("setup: the Giant is Bob's") {
                    game.state.getEntity(giant)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                }
                val ability = cardRegistry.getCard("Cruel Sadist")!!.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sadist,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                        xValue = 2,
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the X counters are removed as a cost") {
                    game.plusOneCounters(sadist) shouldBe 1
                }
                game.resolveStack()

                withClue("the Hill Giant (3/3) takes exactly X = 2 damage and survives") {
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                game.plusOneCounters(sadist) shouldBe 1
                game.state.projectedState.getPower(sadist) shouldBe 2
                game.state.getEntity(sadist)!!.has<TappedComponent>() shouldBe true
            }

            test("X can't exceed the counters on it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Cruel Sadist")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sadist = game.findPermanent("Cruel Sadist")!!
                game.state = game.state.updateEntity(sadist) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
                val giant = game.findPermanent("Hill Giant")!!
                val ability = cardRegistry.getCard("Cruel Sadist")!!.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sadist,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                        xValue = 3,
                    )
                )
                withClue("X = 3 with only one counter must be rejected") {
                    (result.error != null) shouldBe true
                }
                game.plusOneCounters(sadist) shouldBe 1
            }
        }
    }
}
