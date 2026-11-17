package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spectra Ward (M15 #36) — {3}{W}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+2 and has protection from each color. This effect doesn't remove
 *   Auras.
 *
 * Rulings (2014-07-18): its protection puts no Aura into the graveyard, Spectra Ward included; but
 * protection from white gained another way does remove Spectra Ward; and colored Aura spells still
 * can't target the creature.
 */
class SpectraWardScenarioTest : ScenarioTestBase() {

    init {
        context("Spectra Ward") {
            test("keeps every Aura already on the creature, its own included") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")   // white, +1/+2
                    .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears") // black, +2/+1
                    .withCardInHand(1, "Spectra Ward")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(1, "Spectra Ward", bears)
                withClue("cast: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("no Aura is removed by Spectra Ward's protection") {
                    game.isOnBattlefield("Spectra Ward") shouldBe true
                    game.isOnBattlefield("Holy Strength") shouldBe true
                    game.isOnBattlefield("Unholy Strength") shouldBe true
                }
                val projected = game.state.projectedState
                withClue("2/2 + 1/2 + 2/1 + 2/2") {
                    projected.getPower(bears) shouldBe 7
                    projected.getToughness(bears) shouldBe 7
                }
                withClue("protection from each color") {
                    listOf("WHITE", "BLUE", "BLACK", "RED", "GREEN").forEach {
                        projected.hasKeyword(bears, "PROTECTION_FROM_$it") shouldBe true
                    }
                }
            }

            test("a colored Aura spell can't target the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Spectra Ward", "Grizzly Bears")
                    .withCardInHand(1, "Holy Strength")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Holy Strength (white) can't target a creature with protection from white") {
                    game.castSpell(1, "Holy Strength", bears).error shouldNotBe null
                }
            }

            test("protection from white gained another way removes Spectra Ward and other white Auras") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Spectra Ward", "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "White Ward", "Grizzly Bears") // pro-white, spares only itself
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions().error shouldBe null

                withClue("White Ward's protection from white removes the white Auras it doesn't spare") {
                    game.isInGraveyard(1, "Spectra Ward") shouldBe true
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                }
                withClue("White Ward stays: its own effect and Spectra Ward's both spare it") {
                    game.isOnBattlefield("White Ward") shouldBe true
                }
                withClue("only Spectra Ward gave protection from black, and it spares Auras") {
                    game.isOnBattlefield("Unholy Strength") shouldBe true
                }
            }
        }
    }
}
