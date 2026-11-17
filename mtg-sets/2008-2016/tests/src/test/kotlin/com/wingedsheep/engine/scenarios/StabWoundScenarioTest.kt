package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Stab Wound: enchanted creature gets -2/-2, and at the beginning of the upkeep of enchanted
 * creature's controller, that player — not the Aura's controller — loses 2 life.
 */
class StabWoundScenarioTest : ScenarioTestBase() {
    init {
        test("shrinks the creature and drains its controller on their upkeep") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardAttachedTo(1, "Stab Wound", "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.state.projectedState.getPower(wurm) shouldBe 4
            game.state.projectedState.getToughness(wurm) shouldBe 2

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
