package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rhys, the Evermore — specifically the
 * "{W}, {T}: Remove any number of counters from target creature you control" activated ability.
 *
 * The ETB persist-grant rides on existing keyword-grant + persist trigger plumbing; the only
 * new mechanic introduced by this card is `RemoveAnyNumberOfCountersEffect`, which is what
 * these tests focus on.
 */
class RhysTheEvermoreScenarioTest : ScenarioTestBase() {

    init {
        context("Rhys, the Evermore — {W}, {T}: Remove any number of counters") {

            test("removes the chosen number of -1/-1 counters from the target") {
                val game = scenario()
                    .withPlayers("Rhys", "Opponent")
                    .withCardOnBattlefield(1, "Rhys, the Evermore")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rhys = game.findPermanent("Rhys, the Evermore")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.state = game.state.updateEntity(bears) { container ->
                    container.with(CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 2))
                }

                val cardDef = cardRegistry.getCard("Rhys, the Evermore")!!
                val activated = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rhys,
                        abilityId = activated.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("activation should succeed") { result.error shouldBe null }

                game.resolveStack()

                withClue("ability pauses for ChooseNumber on the -1/-1 counters") {
                    game.hasPendingDecision() shouldBe true
                }
                game.chooseNumber(2)

                val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                withClue("both -1/-1 counters should be removed") {
                    (counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
                }
            }

            test("prompts for each counter kind in turn and applies each chosen amount") {
                val game = scenario()
                    .withPlayers("Rhys", "Opponent")
                    .withCardOnBattlefield(1, "Rhys, the Evermore")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rhys = game.findPermanent("Rhys, the Evermore")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.state = game.state.updateEntity(bears) { container ->
                    container.with(
                        CountersComponent()
                            .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3)
                            .withAdded(CounterType.MINUS_ONE_MINUS_ONE, 2)
                    )
                }

                val cardDef = cardRegistry.getCard("Rhys, the Evermore")!!
                val activated = cardDef.script.activatedAbilities[0]

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rhys,
                        abilityId = activated.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null

                game.resolveStack()

                // First prompt is one kind; respond, then a second prompt arrives for the other.
                withClue("first prompt should be pending") { game.hasPendingDecision() shouldBe true }
                game.chooseNumber(1) // remove 1 of the first kind
                withClue("second prompt should be pending") { game.hasPendingDecision() shouldBe true }
                game.chooseNumber(2) // remove all of the second kind

                val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                val plus = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                val minus = counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0
                withClue("exactly one kind retains 2 counters and the other is empty") {
                    // Counter map iteration order is map-defined, so just assert the
                    // total bookkeeping rather than guessing which kind was prompted first.
                    (plus + minus) shouldBe 2
                    (plus == 0 || minus == 0) shouldBe true
                }
            }

            test("no-op when target has no counters") {
                val game = scenario()
                    .withPlayers("Rhys", "Opponent")
                    .withCardOnBattlefield(1, "Rhys, the Evermore")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rhys = game.findPermanent("Rhys, the Evermore")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Rhys, the Evermore")!!
                val activated = cardDef.script.activatedAbilities[0]

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rhys,
                        abilityId = activated.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null

                game.resolveStack()

                withClue("nothing to choose — no decision should be pending") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
