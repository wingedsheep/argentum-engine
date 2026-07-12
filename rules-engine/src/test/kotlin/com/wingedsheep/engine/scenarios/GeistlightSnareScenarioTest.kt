package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Geistlight Snare (VOW #60) — {2}{U} Instant.
 *
 *   This spell costs {1} less to cast if you control a Spirit. It also costs {1} less to cast if
 *   you control an enchantment.
 *   Counter target spell unless its controller pays {3}.
 *
 * Exercises the simplest meaningful branch of the "counter unless pays" effect: the targeted
 * spell's controller has spent all their mana casting it, so when Geistlight Snare resolves they
 * cannot pay {3} and the spell is auto-countered (no payment decision needed).
 */
class GeistlightSnareScenarioTest : ScenarioTestBase() {

    init {
        context("Geistlight Snare — counter target spell unless its controller pays {3}") {

            test("auto-counters when the spell's controller has no mana left to pay {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Geistlight Snare")
                    .withLandsOnBattlefield(1, "Island", 3) // {2}{U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2) // exactly {1}{G}, none left over
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(2, "Grizzly Bears")
                withClue("Casting Grizzly Bears should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.passPriority() // let player 1 respond

                val snareResult = game.castSpellTargetingStackSpell(1, "Geistlight Snare", "Grizzly Bears")
                withClue("Casting Geistlight Snare should succeed: ${snareResult.error}") {
                    snareResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should be countered (in player 2's graveyard, not on battlefield)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
