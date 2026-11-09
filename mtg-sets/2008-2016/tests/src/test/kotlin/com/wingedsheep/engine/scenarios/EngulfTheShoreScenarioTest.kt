package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engulf the Shore (SOI #58) — {3}{U} Instant
 * "Return to their owners' hands all creatures with toughness less than or equal to the number
 *  of Islands you control."
 *
 * What's worth proving is the late-bound threshold inside a non-targeted mass bounce: the
 * `CompareNumericProperty(TOUGHNESS, LTE, landsWithSubtype(Island))` filter must count Islands
 * the *caster* controls (by subtype, so a nonbasic Island like Underground Sea counts and the
 * opponent's Islands don't), and compare against each creature's *projected* toughness at
 * resolution (a Giant Growth that resolved first saves the creature).
 */
class EngulfTheShoreScenarioTest : ScenarioTestBase() {

    init {
        context("Engulf the Shore") {

            test("bounces creatures with toughness at most the caster's Island count, read at resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Engulf the Shore")
                    .withCardInHand(1, "Giant Growth")
                    // Three Islands for Player 1: two basics and a nonbasic with the Island subtype.
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Underground Sea", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    // The opponent's Islands must not raise the threshold.
                    .withLandsOnBattlefield(2, "Island", 3)
                    // 2/2, pumped to 5/5 before Engulf resolves — survives.
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    // 3/3 — toughness equal to the Island count, so it is returned.
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    // 2/4 — toughness above the count, stays.
                    .withCardOnBattlefield(2, "Giant Spider", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                val growth = game.castSpell(1, "Giant Growth", bears)
                withClue("Giant Growth should cast: ${growth.error}") { growth.error shouldBe null }
                game.resolveStack()

                val engulf = game.castSpell(1, "Engulf the Shore")
                withClue("Engulf the Shore should cast: ${engulf.error}") { engulf.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant (toughness 3 = three Islands you control) returns to its owner's hand") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
                withClue("Giant Spider (toughness 4) stays: the opponent's Islands don't count") {
                    game.isOnBattlefield("Giant Spider") shouldBe true
                }
                withClue("Grizzly Bears pumped to 5/5 stays: projected toughness is compared") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
            }

            test("with fewer Islands, a creature it would otherwise catch survives") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Engulf the Shore")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val engulf = game.castSpell(1, "Engulf the Shore")
                withClue("Engulf the Shore should cast: ${engulf.error}") { engulf.error shouldBe null }
                game.resolveStack()

                withClue("one Island: neither the 2/2 nor the 3/3 is returned") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
