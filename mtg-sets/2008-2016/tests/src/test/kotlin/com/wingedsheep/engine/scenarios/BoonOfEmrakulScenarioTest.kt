package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Boon of Emrakul (EMN #81) — {2}{B} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +3/-3.
 */
class BoonOfEmrakulScenarioTest : ScenarioTestBase() {

    init {
        context("Boon of Emrakul") {

            test("kills a creature with three or less toughness") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boon of Emrakul")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Boon of Emrakul", targetId = bears)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("gives a bigger creature +3/-3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boon of Emrakul")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Boon of Emrakul", targetId = wurm).error shouldBe null
                game.resolveStack()

                // Craw Wurm is 6/4.
                game.state.projectedState.getPower(wurm) shouldBe 9
                game.state.projectedState.getToughness(wurm) shouldBe 1
            }
        }
    }
}
