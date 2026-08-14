package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Roadside Blowout (DFT #58).
 *
 * Roadside Blowout {2}{U} — Sorcery
 * This spell costs {2} less to cast if it targets a permanent with mana value 1.
 * Return target creature or Vehicle an opponent controls to its owner's hand.
 * Draw a card.
 *
 * The load-bearing claim is the conditional discount: with exactly one Island available the spell
 * is castable *only* if the chosen target has mana value 1, so each test's ability to pay is the
 * assertion. Engine Rat ({B}, mana value 1) unlocks it; Grizzly Bears ({1}{G}, mana value 2)
 * does not.
 */
class RoadsideBlowoutScenarioTest : ScenarioTestBase() {

    init {
        context("Roadside Blowout") {

            test("targeting a mana value 1 permanent costs {U} and still bounces and draws") {
                val game = blowoutGame(islands = 1)
                val rat = game.findPermanent("Engine Rat")!!
                val handBefore = game.handSize(1)

                val result = game.castSpell(1, "Roadside Blowout", rat)
                withClue("the {2} discount should make one Island enough: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("the creature went back to its owner's hand") {
                    game.isOnBattlefield("Engine Rat") shouldBe false
                    game.isInHand(2, "Engine Rat") shouldBe true
                }
                withClue("Blowout left hand (-1 for itself, +1 for the draw)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("targeting a mana value 2 permanent gets no discount") {
                val game = blowoutGame(islands = 1)
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.castSpell(1, "Roadside Blowout", bears)
                withClue("mana value 2 doesn't match the filter, so {2}{U} is unpayable off one Island") {
                    result.error shouldNotBe null
                }
                withClue("nothing happened") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("with full mana it happily targets a mana value 2 permanent") {
                val game = blowoutGame(islands = 3)
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.castSpell(1, "Roadside Blowout", bears)
                withClue("paying the printed {2}{U}: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
        }
    }

    /** Blowout in hand, [islands] Islands, and both a mana value 1 and a mana value 2 opposing creature. */
    private fun blowoutGame(islands: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Roadside Blowout")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardOnBattlefield(2, "Engine Rat", summoningSickness = false)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        repeat(6) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
