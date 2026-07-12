package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vampire's Kiss (VOW #136) — {1}{B} Sorcery.
 *
 *   Target player loses 2 life and you gain 2 life. Create two Blood tokens.
 *
 * Exercises the composite: the target player loses 2 life, the caster gains 2 life, and two
 * Blood tokens are created under the caster's control.
 */
class VampiresKissScenarioTest : ScenarioTestBase() {

    init {
        context("Vampire's Kiss — drain 2 + two Blood tokens") {

            test("target player loses 2 life, caster gains 2 life, two Blood tokens are created") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vampire's Kiss")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Vampire's Kiss", 2).error shouldBe null
                game.resolveStack()

                withClue("Target player (2) loses 2 life (20 -> 18)") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Caster (1) gains 2 life (20 -> 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Two Blood tokens are created") {
                    game.findPermanents("Blood").size shouldBe 2
                }
            }
        }
    }
}
