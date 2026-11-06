package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Commencement of Festivities — {1}{G} instant, "Prevent all combat damage that
 * would be dealt to players this turn."
 *
 * Pins the recipient narrowing that sets it apart from Fog: combat damage to players is prevented,
 * while combat damage between creatures is still dealt.
 */
class CommencementOfFestivitiesScenarioTest : ScenarioTestBase() {

    init {
        context("Commencement of Festivities") {

            test("combat damage to the player is prevented, combat damage to creatures is not") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Commencement of Festivities")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Commencement of Festivities")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("the unblocked Hill Giant's 3 combat damage to the player is prevented") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("combat damage to creatures is still dealt, so the blocking Lions die to the Bears") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
            }
        }
    }
}
