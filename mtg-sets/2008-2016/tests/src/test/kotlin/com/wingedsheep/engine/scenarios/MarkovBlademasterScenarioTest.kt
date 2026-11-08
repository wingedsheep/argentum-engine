package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Markov Blademaster (DKA #96) — {1}{R}{R} 1/1 double strike.
 * "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
 *
 * Unblocked, it triggers in both damage steps: the first-strike hit's counter makes the
 * regular-damage hit deal 2.
 */
class MarkovBlademasterScenarioTest : ScenarioTestBase() {
    init {
        test("an unblocked attack triggers in each damage step, growing between hits") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Markov Blademaster")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val blademaster = game.findPermanent("Markov Blademaster")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Markov Blademaster" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("1 first-strike damage, then 2 regular damage after the first counter") {
                game.getLifeTotal(2) shouldBe 17
            }
            game.state.getEntity(blademaster)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(blademaster) shouldBe 3
        }
    }
}
