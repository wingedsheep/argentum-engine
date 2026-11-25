package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Battleflight Eagle (M13 #7) — {4}{W} 2/2 Creature — Bird.
 *   Flying
 *   When this creature enters, target creature gets +2/+2 and gains flying until end of turn.
 */
class BattleflightEagleScenarioTest : ScenarioTestBase() {
    init {
        test("entering gives target creature +2/+2 and flying") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Battleflight Eagle")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Battleflight Eagle").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(game.findPermanent("Battleflight Eagle")!!, Keyword.FLYING) shouldBe true
        }
    }
}
