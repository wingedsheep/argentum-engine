package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Decimator Beetle: "Whenever this creature attacks, remove a -1/-1 counter from target creature you
 * control and put a -1/-1 counter on up to one target creature defending player controls."
 *
 * Ruling (2017-04-18): the first target needn't have a -1/-1 counter — the opposing creature still
 * gets its counter even when there's nothing to remove.
 */
class DecimatorBeetleScenarioTest : ScenarioTestBase() {

    init {
        fun board(): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Decimator Beetle")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.minusCounters(name: String): Int =
            state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
                ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

        fun TestGame.attackTargeting(mine: String, theirs: String) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Decimator Beetle" to 2)).error shouldBe null
            val decision = getPendingDecision()
            withClue("the attack trigger asks for its two targets: $decision") {
                (decision is ChooseTargetsDecision) shouldBe true
            }
            submitDecision(
                TargetsResponse(
                    decision!!.id,
                    mapOf(0 to listOf(findPermanent(mine)!!), 1 to listOf(findPermanent(theirs)!!))
                )
            ).error shouldBe null
            resolveStack()
        }

        context("Decimator Beetle attack trigger") {
            test("moves a -1/-1 counter from your creature to the defending player's creature") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { container ->
                    container.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)))
                }

                game.attackTargeting("Grizzly Bears", "Hill Giant")

                game.minusCounters("Grizzly Bears") shouldBe 0
                game.minusCounters("Hill Giant") shouldBe 1
            }

            test("still puts the counter on the opposing creature when there was none to remove") {
                val game = board()

                game.attackTargeting("Grizzly Bears", "Hill Giant")

                game.minusCounters("Grizzly Bears") shouldBe 0
                game.minusCounters("Hill Giant") shouldBe 1
            }
        }
    }
}
