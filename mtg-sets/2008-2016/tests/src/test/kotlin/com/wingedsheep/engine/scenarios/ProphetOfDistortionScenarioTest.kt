package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Prophet of Distortion — {U} 1/2 Eldrazi Drone.
 * "Devoid. {3}{C}: Draw a card."
 */
class ProphetOfDistortionScenarioTest : ScenarioTestBase() {
    init {
        val drawId = cardRegistry.getCard("Prophet of Distortion")!!.script.activatedAbilities.single().id

        fun board(land: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Prophet of Distortion")
            .withLandsOnBattlefield(1, land, 4)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("{3}{C} draws a card; devoid makes it colorless") {
            val game = board("Wastes")
            val prophet = game.findPermanent("Prophet of Distortion")!!
            game.state.projectedState.getColors(prophet) shouldBe emptySet()

            val result = game.execute(ActivateAbility(game.player1Id, prophet, drawId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 1
        }

        test("colored mana can't pay the {C}") {
            val game = board("Island")
            val prophet = game.findPermanent("Prophet of Distortion")!!

            game.execute(ActivateAbility(game.player1Id, prophet, drawId)).error shouldNotBe null
            game.handSize(1) shouldBe 0
        }
    }
}
