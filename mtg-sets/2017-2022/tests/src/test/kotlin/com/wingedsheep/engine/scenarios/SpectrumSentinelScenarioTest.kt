package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Spectrum Sentinel (BRO #244) — protection from multicolored; whenever a nonbasic land an
 * opponent controls enters, you gain 1 life.
 */
class SpectrumSentinelScenarioTest : ScenarioTestBase() {

    private fun game(land: String): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Spectrum Sentinel")
        .withCardInHand(2, land)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("an opponent's nonbasic land entering gains you 1 life") {
            val game = game("Underground River")
            val land = game.findCardsInHand(2, "Underground River").single()
            game.execute(PlayLand(game.player2Id, land)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 21
            game.getLifeTotal(2) shouldBe 20
        }

        test("an opponent's basic land does not trigger") {
            val game = game("Island")
            val land = game.findCardsInHand(2, "Island").single()
            game.execute(PlayLand(game.player2Id, land)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
        }
    }
}
