package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kozilek's Shrieker — {2}{B} 3/2 Eldrazi Drone. "Devoid. {C}: This creature gets +1/+0 and gains
 * menace until end of turn."
 */
class KozileksShriekerScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Kozilek's Shrieker")!!.script.activatedAbilities.single().id

        test("devoid, and {C} gives +1/+0 and menace") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kozilek's Shrieker")
                .withLandsOnBattlefield(1, "Wastes", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val shrieker = game.findPermanent("Kozilek's Shrieker")!!
            game.state.projectedState.getColors(shrieker) shouldBe emptySet()
            game.state.projectedState.hasKeyword(shrieker, Keyword.MENACE) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, shrieker, pumpId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(shrieker) shouldBe 4
            game.state.projectedState.getToughness(shrieker) shouldBe 2
            game.state.projectedState.hasKeyword(shrieker, Keyword.MENACE) shouldBe true
        }
    }
}
