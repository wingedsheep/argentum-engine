package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Voldaren Epicure (VOW #182) — {R} Creature — Vampire, 1/1.
 *
 *   When this creature enters, it deals 1 damage to each opponent. Create a Blood token.
 *
 * Exercises the ETB composite: each opponent takes 1 damage and a Blood token is created for
 * the caster.
 */
class VoldarenEpicureScenarioTest : ScenarioTestBase() {

    init {
        context("Voldaren Epicure ETB") {

            test("entering deals 1 damage to each opponent and creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Voldaren Epicure")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Voldaren Epicure").error shouldBe null
                game.resolveStack()

                withClue("Voldaren Epicure is on the battlefield as a 1/1") {
                    val epicure = game.findPermanent("Voldaren Epicure")
                    (epicure != null) shouldBe true
                    game.state.projectedState.getPower(epicure!!) shouldBe 1
                    game.state.projectedState.getToughness(epicure) shouldBe 1
                }
                withClue("Opponent takes 1 damage (20 -> 19)") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("A Blood token is created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
