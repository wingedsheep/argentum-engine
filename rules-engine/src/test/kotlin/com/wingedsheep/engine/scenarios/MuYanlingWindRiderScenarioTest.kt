package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Mu Yanling, Wind Rider (DFT #52) — {2}{U}{U} Legendary Creature — Human Wizard
 * Pilot (2/4).
 *
 * "When Mu Yanling enters, create a 3/2 colorless Vehicle artifact token with crew 1.
 *  Vehicles you control have flying.
 *  Whenever one or more creatures you control with flying deal combat damage to a player, draw a
 *  card."
 *
 * Covers the new predefined `Vehicle` token (a *noncreature* artifact with printed P/T and crew 1),
 * the blanket flying grant over Vehicles — which has to reach an uncrewed, noncreature Vehicle —
 * and the batched draw trigger.
 */
class MuYanlingWindRiderScenarioTest : ScenarioTestBase() {

    init {
        context("Mu Yanling, Wind Rider") {

            test("entering creates a 3/2 Vehicle token that is not a creature but does have flying") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mu Yanling, Wind Rider")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mu Yanling, Wind Rider").error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Vehicle")
                withClue("the ETB trigger created the Vehicle token") { token shouldNotBe null }

                val projected = game.state.projectedState
                withClue("an uncrewed Vehicle is a noncreature artifact") {
                    projected.isCreature(token!!) shouldBe false
                }
                withClue("\"Vehicles you control have flying\" reaches the uncrewed token too") {
                    projected.hasKeyword(token!!, Keyword.FLYING) shouldBe true
                }
                withClue("the token is the printed 3/2") {
                    projected.getPower(token!!) shouldBe 3
                    projected.getToughness(token) shouldBe 2
                }
            }

            test("a flier connecting draws exactly one card, and a groundling draws none") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Mu Yanling, Wind Rider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Wind Drake", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)
                game.declareAttackers(mapOf("Wind Drake" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.resolveStack()

                withClue("the flying attacker connected, so Mu Yanling drew one card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("a nonflying attacker connecting draws nothing") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Mu Yanling, Wind Rider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.resolveStack()

                withClue("no flier connected, so the trigger never fired") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("two fliers hitting the same player still draw only one card (batch trigger)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Mu Yanling, Wind Rider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Wind Drake", summoningSickness = false)
                    .withCardOnBattlefield(1, "Storm Crow", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)
                game.declareAttackers(mapOf("Wind Drake" to 2, "Storm Crow" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.resolveStack()

                withClue("CR 603.2c — one trigger per damaged player, not per creature") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
