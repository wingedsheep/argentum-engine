package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Essence Depleter — {2}{B} 2/3 Eldrazi Drone.
 * "Devoid. {1}{C}: Target opponent loses 1 life and you gain 1 life."
 */
class EssenceDepleterScenarioTest : ScenarioTestBase() {
    init {
        val drainId = cardRegistry.getCard("Essence Depleter")!!.script.activatedAbilities.single().id

        fun board(land: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Essence Depleter")
            .withLandsOnBattlefield(1, land, 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("{1}{C} drains the target opponent for 1; devoid makes it colorless") {
            val game = board("Wastes")
            val depleter = game.findPermanent("Essence Depleter")!!
            game.state.projectedState.getColors(depleter) shouldBe emptySet()

            val result = game.execute(
                ActivateAbility(game.player1Id, depleter, drainId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }

        test("colored mana can't pay the {C}") {
            val game = board("Swamp")
            val depleter = game.findPermanent("Essence Depleter")!!

            game.execute(
                ActivateAbility(game.player1Id, depleter, drainId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldNotBe null
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
