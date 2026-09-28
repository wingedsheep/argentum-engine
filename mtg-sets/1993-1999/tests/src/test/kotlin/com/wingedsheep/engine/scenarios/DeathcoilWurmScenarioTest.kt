package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deathcoil Wurm — "You may have this creature assign its combat damage as though it weren't blocked."
 */
class DeathcoilWurmScenarioTest : ScenarioTestBase() {

    private fun blockedCombat(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Deathcoil Wurm")
            .withCardOnBattlefield(2, "Wall of Granite")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Deathcoil Wurm" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Wall of Granite" to listOf("Deathcoil Wurm"))).error shouldBe null
        return game
    }

    init {
        context("Deathcoil Wurm") {

            test("assigning as though unblocked hits the player and spares the blocker") {
                val game = blockedCombat()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("7 damage goes to the defending player") {
                    game.getLifeTotal(2) shouldBe 13
                }
                withClue("the blocker took no damage from the Wurm") {
                    game.isOnBattlefield("Wall of Granite") shouldBe true
                }
            }

            test("declining assigns damage to the blocker as normal") {
                val game = blockedCombat()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("no damage reaches the player") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("the 0/7 Wall takes 7 lethal damage") {
                    game.isInGraveyard(2, "Wall of Granite") shouldBe true
                }
            }
        }
    }
}
