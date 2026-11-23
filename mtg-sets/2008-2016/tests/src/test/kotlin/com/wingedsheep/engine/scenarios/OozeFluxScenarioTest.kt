package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.gtc.cards.OozeFlux
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ooze Flux (GTC #128) — {3}{G} Enchantment.
 *
 *   {1}{G}, Remove one or more +1/+1 counters from among creatures you control: Create an X/X
 *   green Ooze creature token, where X is the number of +1/+1 counters removed this way.
 *
 * The cost-linked amount is the interesting part: the removal is spread across several creatures,
 * the token's size is the total removed, and "one or more" forbids X = 0.
 */
class OozeFluxScenarioTest : ScenarioTestBase() {

    private val abilityId = OozeFlux.activatedAbilities.first().id

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.giveCounters(id: EntityId, count: Int) {
        state = state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to count))) }
    }

    private fun setup(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Ooze Flux")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Ooze Flux") {

            test("counters removed from among two creatures make an Ooze of their total size") {
                val game = setup()
                val flux = game.findPermanent("Ooze Flux")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.giveCounters(bears, 2)
                game.giveCounters(giant, 2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = flux,
                        abilityId = abilityId,
                        xValue = 3,
                        costPayment = AdditionalCostPayment(
                            distributedCounterRemovals = listOf(
                                DistributedCounterRemoval(bears, CounterType.PLUS_ONE_PLUS_ONE.printed, 2),
                                DistributedCounterRemoval(giant, CounterType.PLUS_ONE_PLUS_ONE.printed, 1),
                            )
                        )
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the counters come off as a cost") {
                    game.plusOnes(bears) shouldBe 0
                    game.plusOnes(giant) shouldBe 1
                }
                game.resolveStack()

                val oozes = game.findPermanents("Ooze Token")
                oozes.size shouldBe 1
                val ooze = oozes.single()
                withClue("the token is a green 3/3 Ooze") {
                    game.state.projectedState.getPower(ooze) shouldBe 3
                    game.state.projectedState.getToughness(ooze) shouldBe 3
                    game.state.projectedState.getColors(ooze) shouldBe setOf(Color.GREEN.name)
                    game.state.projectedState.hasSubtype(ooze, "Ooze") shouldBe true
                }
            }

            test("removing zero counters is not allowed") {
                val game = setup()
                val flux = game.findPermanent("Ooze Flux")!!
                game.giveCounters(game.findPermanent("Grizzly Bears")!!, 1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = flux,
                        abilityId = abilityId,
                        xValue = 0,
                        costPayment = AdditionalCostPayment(distributedCounterRemovals = emptyList()),
                    )
                )
                withClue("X = 0 is rejected — the cost says one or more") { result.error shouldNotBe null }
                game.findPermanents("Ooze Token").size shouldBe 0
            }
        }
    }
}
