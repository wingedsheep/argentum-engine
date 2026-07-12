package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sheltering Boughs (VOW #218) — {2}{G} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, draw a card.
 *   Enchanted creature gets +1/+3.
 *
 * Exercises the ETB card draw and the static +1/+3 buff on the enchanted creature.
 */
class ShelteringBoughsScenarioTest : ScenarioTestBase() {

    init {
        context("Sheltering Boughs") {

            test("entering the battlefield draws a card and grants +1/+3 to the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sheltering Boughs")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 target
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Sheltering Boughs", targetId = bears)
                withClue("Casting Sheltering Boughs should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Sheltering Boughs is attached and on the battlefield") {
                    game.isOnBattlefield("Sheltering Boughs") shouldBe true
                }
                withClue("the ETB trigger drew a card (net hand size after casting the aura)") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("Grizzly Bears (2/2) gets +1/+3, becoming 3/5") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
            }
        }
    }
}
