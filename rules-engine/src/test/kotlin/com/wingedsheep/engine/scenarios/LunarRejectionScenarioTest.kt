package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lunar Rejection (VOW #67).
 *
 * {1}{U} Instant — Cleave {3}{U}
 * "Return target [Wolf or Werewolf] creature to its owner's hand. Draw a card."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast is a tribal bounce — it can only return a Wolf or Werewolf; the cleaved cast
 * broadens the target to any creature. Both modes bounce the target and draw a card.
 *
 * Target-only difference: the base [target] carries the "Wolf or Werewolf" subtype restriction and
 * [cleaveTarget] drops it. These tests pin both modes:
 *  - printed cast bounces a Wolf but is an illegal target against a non-Wolf/Werewolf creature, and
 *  - the cleaved cast bounces any creature. The draw is verified in both legal casts.
 */
class LunarRejectionScenarioTest : ScenarioTestBase() {

    init {
        context("Lunar Rejection — printed cast (brackets present)") {

            test("returns a Wolf creature to its owner's hand and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lunar Rejection")
                    .withLandsOnBattlefield(1, "Island", 2) // {1}{U}
                    .withCardOnBattlefield(2, "Packsong Pup", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pup = game.findPermanent("Packsong Pup")!!
                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Lunar Rejection", targetId = pup)
                withClue("A Wolf is a legal target for the printed cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The Wolf is bounced to its owner's hand") {
                    game.isOnBattlefield("Packsong Pup") shouldBe false
                    game.isInHand(2, "Packsong Pup") shouldBe true
                }
                withClue("The caster draws a card (hand was spent on the spell, then +1 from the draw)") {
                    // Started with the spell in hand; cast it (−1), drew a card (+1) → back to start.
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("rejects a non-Wolf/Werewolf creature as an illegal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lunar Rejection")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Lunar Rejection", targetId = bears)
                withClue("A Bear is not a legal target for the printed cast") {
                    cast.error shouldNotBe null
                }
                withClue("The illegally-targeted creature survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Lunar Rejection — cleaved cast (brackets removed)") {

            test("returns any creature to its owner's hand and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lunar Rejection")
                    .withLandsOnBattlefield(1, "Island", 4) // Cleave {3}{U}
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                val cast = game.castSpellWithCleave(1, "Lunar Rejection", targetId = bears)
                withClue("Paying the cleave cost broadens the target to any creature: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The non-Wolf Grizzly Bears is bounced by the cleaved cast") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
                withClue("The caster still draws a card") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
