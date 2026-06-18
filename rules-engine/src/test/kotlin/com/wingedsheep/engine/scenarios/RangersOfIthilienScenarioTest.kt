package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rangers of Ithilien — "When this creature enters, gain control of up to one target creature with
 * lesser power for as long as you control this creature." Exercises the new strict
 * `powerLessThanEntity(Source)` target filter (3/3 Rangers can take a 2/2 but not a 3/3+).
 */
class RangersOfIthilienScenarioTest : ScenarioTestBase() {

    init {
        test("gains control of an opponent's creature with lesser power") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Rangers of Ithilien")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2, power 2 < 3
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Rangers of Ithilien").error shouldBe null
            game.resolveStack() // enters → ETB trigger asks for a (lesser-power) target
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            // Control of the 2/2 transferred to player 1 (the tempt may then pause for a Ring-bearer
            // choice, but the control change has already happened).
            game.state.projectedState.getController(bears) shouldBe game.player1Id
        }
    }
}
