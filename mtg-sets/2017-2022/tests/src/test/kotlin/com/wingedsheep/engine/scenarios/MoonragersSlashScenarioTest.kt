package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Moonrager's Slash (MID #148) — {2}{R} Instant.
 * "This spell costs {2} less to cast if it's night. Moonrager's Slash deals 3 damage to any target."
 *
 * First self-cast cost reduction gated on the day/night designation: pins that the discount
 * applies only at night, not during day and not before the game has a designation.
 */
class MoonragersSlashScenarioTest : ScenarioTestBase() {

    private fun game(dayNight: DayNight?, mountains: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Moonrager's Slash")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.state = game.state.copy(dayNight = dayNight)
        return game
    }

    init {
        test("at night it costs {R} and deals 3 damage") {
            val game = game(DayNight.NIGHT, mountains = 1)
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("one Mountain pays the night-discounted {R}") {
                game.castSpell(1, "Moonrager's Slash", bears).error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("during day there is no discount") {
            val game = game(DayNight.DAY, mountains = 1)
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("{2}{R} can't be paid with one Mountain during day") {
                game.castSpell(1, "Moonrager's Slash", bears).error shouldNotBe null
            }
        }

        test("with no day/night designation there is no discount") {
            val game = game(null, mountains = 1)
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("{2}{R} can't be paid with one Mountain before it's ever day or night") {
                game.castSpell(1, "Moonrager's Slash", bears).error shouldNotBe null
            }

            val paid = game(DayNight.DAY, mountains = 3)
            withClue("three Mountains pay the full {2}{R}") {
                paid.castSpell(1, "Moonrager's Slash", paid.findPermanent("Grizzly Bears")!!).error shouldBe null
            }
        }
    }
}
