package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Perimeter Sergeant (IKO #27) — {2}{W} 3/2 Human Soldier.
 *
 *   Whenever this creature attacks, other Humans you control get +1/+0 until end of turn.
 */
class PerimeterSergeantScenarioTest : ScenarioTestBase() {

    init {
        test("attacking pumps other Humans you control, not itself or non-Humans") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Perimeter Sergeant", summoningSickness = false)
                .withCardOnBattlefield(1, "Blade of the Ninth Watch")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "First Strike Knight")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sergeant = game.findPermanent("Perimeter Sergeant")!!
            val blade = game.findPermanent("Blade of the Ninth Watch")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val knight = game.findPermanent("First Strike Knight")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Perimeter Sergeant" to 2)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(blade) shouldBe 3
            projected.getToughness(blade) shouldBe 1
            projected.getPower(sergeant) shouldBe 3
            projected.getPower(bears) shouldBe 2
            projected.getPower(knight) shouldBe 3
        }
    }
}
