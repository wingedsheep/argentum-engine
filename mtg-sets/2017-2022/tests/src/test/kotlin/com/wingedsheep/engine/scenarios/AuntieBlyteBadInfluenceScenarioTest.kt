package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
 * Auntie Blyte, Bad Influence (J22 #30) — {2}{R} 2/2 flying Devil Advisor.
 *
 *   Whenever a source you control deals damage to you, put that many +1/+1 counters on Auntie Blyte.
 *   {1}{R}, {T}, Remove X +1/+1 counters from Auntie Blyte: It deals X damage to any target.
 *
 * Proves the `youControl()` source filter on the damage-to-you trigger: Auntie's own ability and your
 * own spell feed her, an opponent's spell does not.
 */
class AuntieBlyteBadInfluenceScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Auntie Blyte, Bad Influence") {

            test("pinging yourself with her own ability puts the counters back") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Auntie Blyte, Bad Influence")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val auntie = game.findPermanent("Auntie Blyte, Bad Influence")!!
                game.state = game.state.updateEntity(auntie) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }
                val ability = cardRegistry.getCard("Auntie Blyte, Bad Influence")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = auntie,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        xValue = 2,
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the X counters are removed as a cost") { game.plusOneCounters(auntie) shouldBe 0 }
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                withClue("2 damage from a source you control -> 2 counters") {
                    game.plusOneCounters(auntie) shouldBe 2
                }
            }

            test("your own spell damaging you counts as a source you control") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Auntie Blyte, Bad Influence")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val auntie = game.findPermanent("Auntie Blyte, Bad Influence")!!
                val cast = game.castSpellTargetingPlayer(1, "Lightning Bolt", 1)
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.plusOneCounters(auntie) shouldBe 3
            }

            test("an opponent's source damaging you adds nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Auntie Blyte, Bad Influence")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val auntie = game.findPermanent("Auntie Blyte, Bad Influence")!!
                val cast = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.plusOneCounters(auntie) shouldBe 0
            }
        }
    }
}
