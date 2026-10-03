package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Chillerpillar (MH1 #43, reprinted in J22).
 *
 *   {4}{S}{S}: Monstrosity 2.
 *   As long as this creature is monstrous, it has flying.
 *
 * The two `{S}` pips need two snow sources (CR 107.4h); once monstrous the 3/3 is a 5/5 flyer, and
 * a second activation finds it already monstrous and does nothing (CR 701.37a).
 */
class ChillerpillarScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Chillerpillar")!!.activatedAbilities.first().id

    private fun TestGame.activate() = execute(
        ActivateAbility(playerId = player1Id, sourceId = findPermanent("Chillerpillar")!!, abilityId = abilityId)
    )

    private fun TestGame.counters() = state.getEntity(findPermanent("Chillerpillar")!!)
        ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Chillerpillar") {

            test("monstrosity 2 makes it a monstrous 5/5 with flying, and only once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Chillerpillar")
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val pillar = game.findPermanent("Chillerpillar")!!

                withClue("no flying before it is monstrous") {
                    game.state.projectedState.hasKeyword(pillar, Keyword.FLYING) shouldBe false
                }

                game.activate().error shouldBe null
                game.resolveStack()

                withClue("two +1/+1 counters and the monstrous designation") {
                    game.counters() shouldBe 2
                    game.state.getEntity(pillar)?.has<MonstrousComponent>() shouldBe true
                }
                game.state.projectedState.getPower(pillar) shouldBe 5
                game.state.projectedState.getToughness(pillar) shouldBe 5
                game.state.projectedState.hasKeyword(pillar, Keyword.FLYING) shouldBe true

                withClue("a second activation resolves but adds nothing") {
                    game.activate().error shouldBe null
                    game.resolveStack()
                    game.counters() shouldBe 2
                }
            }

            test("plain lands alone can't pay {S}{S}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Chillerpillar")
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("one snow source isn't enough for two {S}") {
                    val monstrosity = game.getLegalActions(1)
                        .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                    monstrosity.isNotEmpty() shouldBe true
                    monstrosity.none { it.isAffordable } shouldBe true
                }
            }
        }
    }
}
