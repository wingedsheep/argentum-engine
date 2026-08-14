package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ticket Tortoise (DFT #245) — {2} Artifact Creature — Turtle, 3/1, Defender.
 *
 *   "When this creature enters, if an opponent controls more lands than you, you create a
 *    Treasure token."
 *
 * Covers both sides of the intervening-if clause (CR 603.4): the Treasure appears only when an
 * opponent really is ahead on lands as the trigger resolves.
 */
class TicketTortoiseScenarioTest : ScenarioTestBase() {

    init {
        context("Ticket Tortoise enters trigger") {

            test("creates a Treasure when an opponent controls more lands than you") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ticket Tortoise")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("No Treasure on the battlefield before the tortoise enters") {
                    game.findPermanent("Treasure") shouldBe null
                }

                game.castSpell(1, "Ticket Tortoise")
                game.resolveStack()

                withClue("The tortoise resolved onto the battlefield") {
                    game.findPermanent("Ticket Tortoise") shouldNotBe null
                }
                withClue("Opponent has 4 lands vs your 2, so the enters trigger makes a Treasure") {
                    game.findPermanent("Treasure") shouldNotBe null
                }
            }

            test("creates nothing when no opponent controls more lands than you") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ticket Tortoise")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ticket Tortoise")
                game.resolveStack()

                withClue("The tortoise still enters") {
                    game.findPermanent("Ticket Tortoise") shouldNotBe null
                }
                withClue("Land counts are tied, so the intervening-if fails and no Treasure is made") {
                    game.findPermanent("Treasure") shouldBe null
                }
            }
        }
    }
}
