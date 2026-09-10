package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Smite (STH) — "Destroy target blocked creature."
 *
 * Smite is the first card on the new `Targets.BlockedCreature` filter, so the tests cover the
 * filter rather than the destruction: a blocked attacker dies, an unblocked one isn't even a legal
 * target, and — the case that separates the durable combat status from a live "is anything
 * blocking it right now" scan — an attacker whose only blocker has already died is still blocked
 * (CR 509.1h).
 */
class SmiteScenarioTest : ScenarioTestBase() {

    init {
        context("Smite") {

            test("destroys a blocked attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Smite")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser"))).error shouldBe null

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castSpell(2, "Smite", courser)
                withClue("Smite should target the blocked attacker: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("the blocked attacker is destroyed") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }
            }

            test("cannot target an unblocked attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(2, "Smite")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castSpell(2, "Smite", courser)
                withClue("an attacker nobody blocked is not a blocked creature") {
                    cast.error shouldNotBe null
                    // Guard against passing for the wrong reason: it must be the target that's
                    // illegal, not the timing.
                    cast.error shouldNotBe "You don't have priority"
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }

            test("an attacker stays blocked after its only blocker dies (CR 509.1h)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Smite")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser"))).error shouldBe null

                // Blow up our own blocker: the attacker is no longer being blocked by anything,
                // but it is still a blocked creature for the rest of combat.
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castSpell(2, "Smite", courser)
                withClue("blocked status survives the blocker leaving: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                game.isInGraveyard(1, "Centaur Courser") shouldBe true
            }
        }
    }
}
