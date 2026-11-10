package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maw of Kozilek — {3}{R} 2/5 Eldrazi Drone. "Devoid. {C}: This creature gets +2/-2 until end of
 * turn."
 */
class MawofKozilekScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Maw of Kozilek")!!.script.activatedAbilities.single().id

        test("devoid, and each {C} gives +2/-2 (repeatable)") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Maw of Kozilek")
                .withLandsOnBattlefield(1, "Wastes", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val maw = game.findPermanent("Maw of Kozilek")!!
            game.state.projectedState.getColors(maw) shouldBe emptySet()

            val first = game.execute(ActivateAbility(game.player1Id, maw, pumpId))
            withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getPower(maw) shouldBe 4
            game.state.projectedState.getToughness(maw) shouldBe 3

            val second = game.execute(ActivateAbility(game.player1Id, maw, pumpId))
            withClue("no once-per-turn limit: ${second.error}") { second.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getPower(maw) shouldBe 6
            game.state.projectedState.getToughness(maw) shouldBe 1
        }
    }
}
