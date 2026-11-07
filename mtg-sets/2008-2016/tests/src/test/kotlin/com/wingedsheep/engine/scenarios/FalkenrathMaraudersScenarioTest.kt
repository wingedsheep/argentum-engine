package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Falkenrath Marauders (ISD #141) — {3}{R}{R} 2/2 flying, haste.
 * "Whenever this creature deals combat damage to a player, put two +1/+1 counters on it."
 */
class FalkenrathMaraudersScenarioTest : ScenarioTestBase() {
    init {
        test("cast and attack the same turn; connecting puts two +1/+1 counters on it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Falkenrath Marauders")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Falkenrath Marauders").error shouldBe null
            game.resolveStack()
            val marauders = game.findPermanent("Falkenrath Marauders")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets it attack the turn it was cast") {
                game.declareAttackers(mapOf("Falkenrath Marauders" to 2)).error shouldBe null
            }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.state.getEntity(marauders)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(marauders) shouldBe 4
            game.state.projectedState.getToughness(marauders) shouldBe 4
        }
    }
}
