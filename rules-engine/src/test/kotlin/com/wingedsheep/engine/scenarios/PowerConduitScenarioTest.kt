package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.PowerConduit
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Power Conduit (MRD #229, {2} Artifact).
 *
 *   {T}, Remove a counter from a permanent you control: Choose one —
 *   • Put a charge counter on target artifact.
 *   • Put a +1/+1 counter on target creature.
 *
 * The Conduit converts counters, so the cost is the interesting half: the counter removed is of
 * *any* kind (`counterType = null` on the `Costs.RemoveCounters` atom) off *any* permanent you
 * control — including the Conduit itself, which is what makes it a loop rather than a one-shot.
 * The two modes are a printed "Choose one —" on an activated ability, so the mode is picked on
 * resolution and only the chosen mode demands a target.
 */
class PowerConduitScenarioTest : ScenarioTestBase() {

    private val abilityId = PowerConduit.activatedAbilities.first().id

    private fun countersOn(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun giveCounter(game: TestGame, id: EntityId, type: CounterType, count: Int) {
        game.state = game.state.updateEntity(id) { container ->
            container.with(CountersComponent(mapOf(type to count)))
        }
    }

    init {
        context("Power Conduit") {

            test("mode 1 turns a +1/+1 counter on a creature into a charge counter on an artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Power Conduit")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conduit = game.findPermanent("Power Conduit")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                giveCounter(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

                val act = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = conduit,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(
                            distributedCounterRemovals = listOf(
                                DistributedCounterRemoval(bears, Counters.PLUS_ONE_PLUS_ONE, 1)
                            )
                        )
                    )
                )
                withClue("Activating should succeed: ${act.error}") { act.error shouldBe null }

                withClue("Both halves of the cost are paid on activation") {
                    game.state.getEntity(conduit)?.has<TappedComponent>() shouldBe true
                    countersOn(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                }

                game.resolveStack()

                val mode = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a mode choice; got ${game.state.pendingDecision}")
                withClue("Choose one — exactly two modes") { mode.options.size shouldBe 2 }
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))

                val targets = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a target choice after the mode; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(bonesplitter))))
                game.resolveStack()

                withClue("The artifact gets the charge counter") {
                    countersOn(game, bonesplitter, CounterType.CHARGE) shouldBe 1
                }
                withClue("Only the chosen mode happens — no +1/+1 counter was handed out") {
                    countersOn(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                }
            }

            test("mode 2 recycles the Conduit's own charge counter into a +1/+1 counter") {
                // The counter may be of any kind and may come off any permanent you control —
                // the Conduit included, which is what lets it feed itself.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Power Conduit")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conduit = game.findPermanent("Power Conduit")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                giveCounter(game, conduit, CounterType.CHARGE, 1)

                val act = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = conduit,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(
                            distributedCounterRemovals = listOf(
                                DistributedCounterRemoval(conduit, Counters.CHARGE, 1)
                            )
                        )
                    )
                )
                withClue("A charge counter off the Conduit itself is a legal payment: ${act.error}") {
                    act.error shouldBe null
                }
                countersOn(game, conduit, CounterType.CHARGE) shouldBe 0

                game.resolveStack()

                val mode = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a mode choice; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))

                val targets = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a target choice after the mode; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(bears))))
                game.resolveStack()

                withClue("Grizzly Bears grows to a 3/3") {
                    countersOn(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("with no counters anywhere the ability can't be activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Power Conduit")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conduit = game.findPermanent("Power Conduit")!!

                val act = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = conduit, abilityId = abilityId)
                )
                withClue("The counter removal is a cost, not an optional rider") {
                    act.isSuccess shouldBe false
                }
                withClue("A rejected activation leaves the Conduit untapped") {
                    game.state.getEntity(conduit)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
