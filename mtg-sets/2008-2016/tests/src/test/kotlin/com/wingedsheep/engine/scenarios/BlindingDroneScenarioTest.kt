package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blinding Drone — {1}{U} 1/3 Eldrazi Drone. "Devoid. {C}, {T}: Tap target creature."
 */
class BlindingDroneScenarioTest : ScenarioTestBase() {
    init {
        val tapId = cardRegistry.getCard("Blinding Drone")!!.script.activatedAbilities.single().id

        fun board(land: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Blinding Drone")
            .withLandsOnBattlefield(1, land, 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("devoid: the blue-costed Drone is colorless") {
            val game = board("Wastes")
            game.state.projectedState.getColors(game.findPermanent("Blinding Drone")!!) shouldBe emptySet()
        }

        test("{C}, {T}: taps target creature") {
            val game = board("Wastes")
            val drone = game.findPermanent("Blinding Drone")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(game.player1Id, drone, tapId, listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(drone)?.has<TappedComponent>() shouldBe true
        }

        test("colored mana can't pay the {C} cost") {
            val game = board("Island")
            val drone = game.findPermanent("Blinding Drone")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, drone, tapId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
        }
    }
}
