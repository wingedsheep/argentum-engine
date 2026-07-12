package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fierce Retribution (VOW #13).
 *
 * {1}{W} Instant — Cleave {5}{W}
 * "Destroy target [attacking] creature."
 *
 * Cleave (CR 702.148) is an alternative casting cost; paying it removes the words in square
 * brackets. Fierce Retribution is modelled structurally: the printed [target] carries the
 * bracketed "attacking" restriction, and [cleaveTarget] carries the brackets-removed "any
 * creature" requirement. These tests pin both modes:
 *  - printed cast destroys an *attacking* creature but is an illegal target against a
 *    non-attacking one, and
 *  - the cleaved cast destroys any creature regardless of combat status.
 */
class FierceRetributionScenarioTest : ScenarioTestBase() {

    init {
        context("Fierce Retribution — printed cast (brackets present)") {

            test("destroys an attacking creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fierce Retribution")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W}
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                val cast = game.castSpell(1, "Fierce Retribution", targetId = bears)
                withClue("An attacking creature is a legal target for the printed cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The attacking Grizzly Bears is destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("rejects a non-attacking creature as an illegal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fierce Retribution")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Fierce Retribution", targetId = bears)
                withClue("A creature that isn't attacking is not a legal target for the printed cast") {
                    cast.error shouldNotBe null
                }
                withClue("The illegally-targeted creature survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Fierce Retribution — cleaved cast (brackets removed)") {

            test("destroys any creature, even one that isn't attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fierce Retribution")
                    .withLandsOnBattlefield(1, "Plains", 6) // Cleave {5}{W}
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpellWithCleave(1, "Fierce Retribution", targetId = bears)
                withClue("Paying the cleave cost broadens the target to any creature: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The non-attacking Grizzly Bears is destroyed by the cleaved cast") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
