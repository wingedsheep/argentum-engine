package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Alchemist's Retrieval (VOW #47).
 *
 * {U} Instant — Cleave {1}{U}
 * "Return target nonland permanent [you control] to its owner's hand."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast can only bounce a nonland permanent *you control* (a defensive save); the cleaved
 * cast bounces any nonland permanent (a tempo play against an opponent).
 *
 * Target-only difference: the base [target] carries "you control" and [cleaveTarget] drops it —
 * the return-to-hand effect is identical. These tests pin both modes:
 *  - printed cast bounces your own permanent but is an illegal target against an opponent's, and
 *  - the cleaved cast bounces an opponent's permanent.
 */
class AlchemistsRetrievalScenarioTest : ScenarioTestBase() {

    init {
        context("Alchemist's Retrieval — printed cast (brackets present)") {

            test("returns a nonland permanent you control to your hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Retrieval")
                    .withLandsOnBattlefield(1, "Island", 1) // {U}
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Alchemist's Retrieval", targetId = bears)
                withClue("A permanent you control is a legal target for the printed cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Your Grizzly Bears is bounced to your hand") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("rejects a permanent you don't control as an illegal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Retrieval")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Alchemist's Retrieval", targetId = bears)
                withClue("An opponent's permanent is not a legal target for the printed cast") {
                    cast.error shouldNotBe null
                }
                withClue("The opponent's Grizzly Bears survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Alchemist's Retrieval — cleaved cast (brackets removed)") {

            test("returns any nonland permanent, even one you don't control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Alchemist's Retrieval")
                    .withLandsOnBattlefield(1, "Island", 2) // Cleave {1}{U}
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpellWithCleave(1, "Alchemist's Retrieval", targetId = bears)
                withClue("Paying the cleave cost broadens the target to any nonland permanent: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The opponent's Grizzly Bears is bounced to its owner's hand") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
