package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Douse in Gloom (GPT #49) — {2}{B} Instant.
 *
 * "Douse in Gloom deals 2 damage to target creature and you gain 2 life."
 *
 * Verifies the spell deals 2 damage to the targeted creature and the caster gains 2 life.
 */
class DouseInGloomScenarioTest : ScenarioTestBase() {

    init {
        context("Douse in Gloom deals damage and gains life") {

            test("deals 2 damage to a creature and the caster gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Douse in Gloom")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    // Centaur Courser is a 3/3, so 2 damage marks it without killing it.
                    .withCardOnBattlefield(2, "Centaur Courser", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val result = game.castSpell(1, "Douse in Gloom", targetId = courser)
                withClue("Casting Douse in Gloom should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Caster should have gained 2 life (20 -> 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Centaur Courser should have 2 damage marked on it") {
                    game.state.getEntity(courser)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }
        }
    }
}
