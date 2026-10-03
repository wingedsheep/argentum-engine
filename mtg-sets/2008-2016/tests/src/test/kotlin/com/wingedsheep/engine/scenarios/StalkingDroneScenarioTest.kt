package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stalking Drone — "Devoid. {C}: This creature gets +1/+2 until end of turn. Activate only once
 * each turn."
 */
class StalkingDroneScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Stalking Drone")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Stalking Drone")
            .withLandsOnBattlefield(1, "Wastes", 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("devoid: the green-costed Drone is colorless") {
            val game = board()
            val drone = game.findPermanent("Stalking Drone")!!
            game.state.projectedState.getColors(drone) shouldBe emptySet()
        }

        test("{C} pumps it +1/+2, and only once each turn") {
            val game = board()
            val drone = game.findPermanent("Stalking Drone")!!

            val first = game.execute(ActivateAbility(game.player1Id, drone, pumpId))
            withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getPower(drone) shouldBe 3
            game.state.projectedState.getToughness(drone) shouldBe 4

            withClue("a second activation in the same turn is illegal even with {C} available") {
                game.execute(ActivateAbility(game.player1Id, drone, pumpId)).error shouldNotBe null
            }
        }

        test("colored mana can't pay the {C} cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Stalking Drone")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val drone = game.findPermanent("Stalking Drone")!!

            game.execute(ActivateAbility(game.player1Id, drone, pumpId)).error shouldNotBe null
            game.state.projectedState.getPower(drone) shouldBe 2
        }
    }
}
