package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Etched Slith (MH3 #91) — {1}{B} 1/1 Artifact Creature — Phyrexian Slith.
 *
 * "Menace
 *  Whenever this creature deals combat damage to a player, put a +1/+1 counter on it. When you do,
 *  you may remove a counter from another target permanent or opponent."
 *
 * The removal is a CR 603.12 reflexive trigger: its target is chosen after the +1/+1 counter lands,
 * and the "may" is answered at that target prompt (a target is the yes, an empty selection declines).
 * "Another" keeps the Slith itself off the target list,
 * and only opponents (not the controller) are legal player targets.
 */
class EtchedSlithScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun TestGame.addCounters(id: EntityId, type: CounterType, count: Int) {
        state = state.updateEntity(id) { c ->
            val existing = c.get<CountersComponent>() ?: CountersComponent()
            c.with(existing.withAdded(type, count))
        }
    }

    /** Pass priority until a decision is pending (or give up). */
    private fun TestGame.advanceToDecision() {
        var safety = 0
        while (getPendingDecision() == null && safety++ < 20) passPriority()
    }

    /** Advance through the mandatory counter and announce the reflexive trigger's target. */
    private fun TestGame.attackToTargetPrompt(): ChooseTargetsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Etched Slith" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        advanceToDecision()
        return getPendingDecision() as? ChooseTargetsDecision
            ?: error("expected a ChooseTargetsDecision, got ${getPendingDecision()}")
    }

    init {
        context("Etched Slith") {

            test("combat damage grows the Slith, then removes a counter from an opposing creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Etched Slith", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val slith = game.findPermanent("Etched Slith")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

                game.attackToTargetPrompt()
                withClue("the +1/+1 counter is put on before the reflexive trigger targets") {
                    game.counters(slith, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                game.getLifeTotal(2) shouldBe 19

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false

                game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.counters(slith, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

            test("can remove a poison counter from the opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Etched Slith", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.addCounters(game.player2Id, CounterType.POISON, 3)

                game.attackToTargetPrompt()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false

                game.counters(game.player2Id, CounterType.POISON) shouldBe 2
            }

            test("the removal is optional") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Etched Slith", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val slith = game.findPermanent("Etched Slith")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

                val prompt = game.attackToTargetPrompt()
                withClue("the target prompt offers the decline") {
                    prompt.targetRequirements.single().minTargets shouldBe 0
                }
                // Declining is an empty target selection: the reflexive trigger never goes on the stack.
                game.selectTargets(emptyList()).error shouldBe null
                game.state.stack.isEmpty() shouldBe true
                game.resolveStack()

                game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.counters(slith, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

            test("the Slith itself and its controller are not legal targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Etched Slith", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val slith = game.findPermanent("Etched Slith")!!
                val ownBears = game.findPermanent("Grizzly Bears")!!

                val decision = game.attackToTargetPrompt()
                val legal = decision.legalTargets[0].orEmpty()
                legal shouldNotContain slith
                legal shouldNotContain game.player1Id
                legal shouldContain game.player2Id
                withClue("\"another target permanent\" includes your own other permanents") {
                    legal shouldContain ownBears
                }
            }
        }
    }
}
