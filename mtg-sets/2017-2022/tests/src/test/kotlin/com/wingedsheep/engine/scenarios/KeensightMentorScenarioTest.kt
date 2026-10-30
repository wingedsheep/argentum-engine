package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Keensight Mentor (IKO #18) — {2}{W} 1/4 Human Cleric.
 *
 *   When this creature enters, put a vigilance counter on target non-Human creature you control.
 *   {1}{W}, {T}: Put a +1/+1 counter on each creature you control with vigilance.
 */
class KeensightMentorScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("enters trigger targets only non-Human creatures you control and grants vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Keensight Mentor")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Blade of the Ninth Watch")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Keensight Mentor").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(bears)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.counters(bears, CounterType.VIGILANCE) shouldBe 1
            game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }

        test("activated ability puts a +1/+1 counter on each creature you control with vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Keensight Mentor", summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mentor = game.findPermanent("Keensight Mentor")!!
            val (myAngel, theirAngel) = game.findAllPermanents("Serra Angel")
                .partition { game.state.projectedState.getController(it) == game.player1Id }
                .let { it.first.single() to it.second.single() }
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Keensight Mentor")!!.activatedAbilities.single().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = mentor, abilityId = abilityId))
                .error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.counters(myAngel, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            game.counters(theirAngel, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            game.counters(mentor, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        }
    }
}
