package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Erratic Apparition (DSK #54) — {2}{U} 1/3 Creature — Spirit, Flying, vigilance.
 *
 * "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room,
 *  this creature gets +1/+1 until end of turn."
 *
 * Exercises the enchantment-enters half of the Eerie ability buffing itself +1/+1.
 */
class ErraticApparitionScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Erratic Apparition — Eerie self-buff") {

            test("is a base 1/3 before any enchantment enters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Erratic Apparition")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apparition = game.findPermanent("Erratic Apparition")!!
                val projected = projector.project(game.state)
                withClue("No enchantment entered — base 1/3") {
                    projected.getPower(apparition) shouldBe 1
                    projected.getToughness(apparition) shouldBe 3
                }
            }

            test("an enchantment you control entering pumps it to 2/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Erratic Apparition")
                    .withCardInHand(1, "Test Enchantment") // {1}{W}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Test Enchantment")
                withClue("Casting Test Enchantment should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val apparition = game.findPermanent("Erratic Apparition")!!
                val projected = projector.project(game.state)
                withClue("Eerie fired — 1/3 + 1/1 = 2/4") {
                    projected.getPower(apparition) shouldBe 2
                    projected.getToughness(apparition) shouldBe 4
                }
            }

            test("an opponent's enchantment entering does NOT pump") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Erratic Apparition")
                    .withCardInHand(2, "Test Enchantment")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apparition = game.findPermanent("Erratic Apparition")!!
                val cast = game.castSpell(2, "Test Enchantment")
                withClue("Opponent casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("no pump — the enchantment isn't controlled by the Apparition's controller") {
                    val projected = projector.project(game.state)
                    projected.getPower(apparition) shouldBe 1
                    projected.getToughness(apparition) shouldBe 3
                }
            }
        }
    }
}
