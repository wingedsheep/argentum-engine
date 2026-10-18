package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Dramatist's Puppet (NEO #244) — {4} Artifact Creature — Construct, 2/4.
 *
 *   When this creature enters, for each kind of counter on target permanent, put another counter
 *   of that kind on it or remove one from it.
 */
class DramatistsPuppetScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent((c.get<CountersComponent>()?.counters ?: emptyMap()) + (type to amount)))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Map<CounterType, Int> =
        game.state.getEntity(id)?.get<CountersComponent>()?.counters.orEmpty().filterValues { it > 0 }

    private fun TestGame.choose(option: String) {
        val decision = getPendingDecision() as ChooseOptionDecision
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(option))).error shouldBe null
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Dramatist's Puppet")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("removes one kind and adds to another on the target, one choice per kind") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 2)
            seed(game, bears, CounterType.STUN, 1)

            game.castSpell(1, "Dramatist's Puppet").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.choose("Remove a +1/+1 counter from it")
            game.choose("Put another stun counter on it")

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.STUN to 2)
            game.state.projectedState.getPower(bears) shouldBe 3
        }

        test("a target with no counters is left alone") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Dramatist's Puppet").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe emptyMap()
            game.isOnBattlefield("Dramatist's Puppet") shouldBe true
        }
    }
}
