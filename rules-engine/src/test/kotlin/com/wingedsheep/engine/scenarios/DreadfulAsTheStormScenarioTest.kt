package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dreadful as the Storm — "Target creature has base power and toughness 5/5 until end of turn.
 * The Ring tempts you." Exercises the SetBasePowerAndToughness facade (Layer 7b set-values) on a
 * creature whose printed stats differ.
 */
class DreadfulAsTheStormScenarioTest : ScenarioTestBase() {

    init {
        test("sets target creature's base power and toughness to 5/5") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Dreadful as the Storm")
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bear) shouldBe 2
            game.state.projectedState.getToughness(bear) shouldBe 2

            game.castSpell(1, "Dreadful as the Storm", bear).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 5
            game.state.projectedState.getToughness(bear) shouldBe 5
        }
    }
}
