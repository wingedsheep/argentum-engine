package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Daybreak Combatants (VOW #153) — {2}{R} Creature — Human Warrior, 2/2, haste.
 *
 *   When this creature enters, target creature gets +2/+0 until end of turn.
 *
 * Exercises the ETB targeted buff: the trigger asks for a target creature and applies +2/+0.
 */
class DaybreakCombatantsScenarioTest : ScenarioTestBase() {

    init {
        context("Daybreak Combatants ETB buff") {

            test("entering gives a target creature +2/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Daybreak Combatants")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 target
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Daybreak Combatants").error shouldBe null
                game.resolveStack() // creature enters → ETB trigger asks for a target

                val result = game.selectTargets(listOf(bears))
                withClue("Targeting a creature is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gets +2/+0 (becomes 4/2)") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("Daybreak Combatants is on the battlefield") {
                    game.isOnBattlefield("Daybreak Combatants") shouldBe true
                }
            }
        }
    }
}
