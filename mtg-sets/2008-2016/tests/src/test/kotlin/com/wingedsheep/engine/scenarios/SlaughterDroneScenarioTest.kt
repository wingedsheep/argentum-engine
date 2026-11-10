package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Slaughter Drone (OGW) — {1}{B} 2/2 Eldrazi Drone.
 * "Devoid. {C}: This creature gains deathtouch until end of turn."
 */
class SlaughterDroneScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Slaughter Drone")!!.script.activatedAbilities.single().id

        fun board(land: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Slaughter Drone")
            .withLandsOnBattlefield(1, land, 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("devoid: the black-costed Drone is colorless and starts without deathtouch") {
            val game = board("Wastes")
            val drone = game.findPermanent("Slaughter Drone")!!
            game.state.projectedState.getColors(drone) shouldBe emptySet()
            game.state.projectedState.hasKeyword(drone, Keyword.DEATHTOUCH) shouldBe false
        }

        test("{C}: gains deathtouch until end of turn") {
            val game = board("Wastes")
            val drone = game.findPermanent("Slaughter Drone")!!

            val result = game.execute(ActivateAbility(game.player1Id, drone, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(drone, Keyword.DEATHTOUCH) shouldBe true
        }

        test("colored mana can't pay the {C} cost") {
            val game = board("Swamp")
            val drone = game.findPermanent("Slaughter Drone")!!

            game.execute(ActivateAbility(game.player1Id, drone, abilityId)).error shouldNotBe null
            game.state.projectedState.hasKeyword(drone, Keyword.DEATHTOUCH) shouldBe false
        }
    }
}
