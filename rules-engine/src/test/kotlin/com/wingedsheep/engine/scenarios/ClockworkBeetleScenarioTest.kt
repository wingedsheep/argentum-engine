package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Clockwork Beetle (MRD #153).
 *
 * {1} Artifact Creature — Insect 0/0
 * "This creature enters with two +1/+1 counters on it.
 *  Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat."
 *
 * The printed base P/T is 0/0, so the counters are the only thing keeping it alive — shedding the
 * last one kills it as a state-based action. That is the edge worth pinning down, alongside the
 * "attacks *or* blocks" shed itself.
 */
class ClockworkBeetleScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        fun plusOnePlusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Clockwork Beetle") {

            test("enters with two +1/+1 counters and is a 2/2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clockwork Beetle")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clockwork Beetle").error shouldBe null
                game.resolveStack()

                val beetle = game.findPermanent("Clockwork Beetle")!!
                withClue("Enters with two +1/+1 counters") {
                    plusOnePlusOne(game, beetle) shouldBe 2
                }
                withClue("Base 0/0 plus two +1/+1 counters projects as a 2/2") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(beetle) shouldBe 2
                    projected.getToughness(beetle) shouldBe 2
                }
            }

            test("sheds a counter at end of combat after attacking, becoming a 1/1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beetle")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val beetle = game.findPermanent("Clockwork Beetle")!!
                game.state = game.state.updateEntity(beetle) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Beetle" to 2)).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+1 counter shed at end of combat (attacked this combat)") {
                    plusOnePlusOne(game, beetle) shouldBe 1
                }
                withClue("With one +1/+1 counter it is a 1/1") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(beetle) shouldBe 1
                    projected.getToughness(beetle) shouldBe 1
                }
            }

            test("shedding its last counter kills it — a 0/0 dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beetle")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val beetle = game.findPermanent("Clockwork Beetle")!!
                // Down to its last counter — a 1/1 going into combat.
                game.state = game.state.updateEntity(beetle) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Beetle" to 2)).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("The last counter is gone, leaving a 0/0 that dies immediately") {
                    game.findPermanent("Clockwork Beetle") shouldBe null
                    game.isInGraveyard(1, "Clockwork Beetle") shouldBe true
                }
            }

            test("does not shed a counter if it neither attacked nor blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beetle")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val beetle = game.findPermanent("Clockwork Beetle")!!
                game.state = game.state.updateEntity(beetle) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("No combat participation → the intervening-if fails and no counter is removed") {
                    plusOnePlusOne(game, beetle) shouldBe 2
                }
            }
        }
    }
}
