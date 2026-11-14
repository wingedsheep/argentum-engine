package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Call of the Full Moon (ORI #134) — {1}{R} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +3/+2 and has trample.
 *   At the beginning of each upkeep, if a player cast two or more spells last turn, sacrifice this Aura.
 *
 * Pins the upkeep check against both rulings: a *single* player must have cast two spells (one each
 * from two players doesn't count), the count includes the non-active player's spells, and it covers
 * the whole previous turn — the Aura itself plus one more spell sacrifices it.
 */
class CallOfTheFullMoonScenarioTest : ScenarioTestBase() {

    private val freeInstant = card("Test Free Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1, EffectTarget.Controller) }
    }

    init {
        cardRegistry.register(freeInstant)

        /** Player 1's main phase, Grizzly Bears on player 1's side, optionally already enchanted. */
        fun game(p1Instants: Int = 0, p2Instants: Int = 0, attached: Boolean = true): ScenarioTestBase.TestGame {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (attached) {
                builder.withCardAttachedTo(1, "Call of the Full Moon", "Grizzly Bears")
            } else {
                builder.withCardInHand(1, "Call of the Full Moon")
                builder.withLandsOnBattlefield(1, "Mountain", 2)
            }
            repeat(p1Instants) { builder.withCardInHand(1, "Test Free Instant") }
            repeat(p2Instants) { builder.withCardInHand(2, "Test Free Instant") }
            repeat(5) { builder.withCardInLibrary(1, "Mountain") }
            repeat(5) { builder.withCardInLibrary(2, "Mountain") }
            return builder.build()
        }

        /** Each player casts their instants during player 1's turn, then the game moves to player 2's upkeep. */
        fun ScenarioTestBase.TestGame.castAndAdvance(p1Instants: Int, p2Instants: Int) {
            repeat(p1Instants) {
                castSpell(1, "Test Free Instant").error shouldBe null
                resolveStack()
            }
            repeat(p2Instants) {
                // Player 1 passes priority on an empty stack, handing it to player 2.
                passPriority()
                castSpell(2, "Test Free Instant").error shouldBe null
                resolveStack()
            }
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            resolveStack()
        }

        context("Call of the Full Moon") {

            test("enchanted creature gets +3/+2 and has trample") {
                val g = game()
                val bears = g.findPermanent("Grizzly Bears")!!
                val projected = g.state.projectedState
                projected.getPower(bears) shouldBe 5
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            test("no spells last turn keeps the Aura") {
                val g = game()
                g.castAndAdvance(0, 0)
                g.state.activePlayerId shouldBe g.player2Id
                g.isOnBattlefield("Call of the Full Moon") shouldBe true
            }

            test("two spells by the active player sacrifice it at the next upkeep") {
                val g = game(p1Instants = 2)
                g.castAndAdvance(2, 0)
                withClue("the trigger is 'each upkeep', so it fires in the opponent's upkeep too") {
                    g.state.activePlayerId shouldBe g.player2Id
                    g.isOnBattlefield("Call of the Full Moon") shouldBe false
                    g.isInGraveyard(1, "Call of the Full Moon") shouldBe true
                }
            }

            test("two spells by the non-active player count too") {
                val g = game(p2Instants = 2)
                g.castAndAdvance(0, 2)
                g.isInGraveyard(1, "Call of the Full Moon") shouldBe true
            }

            test("one spell from each player doesn't sacrifice it") {
                val g = game(p1Instants = 1, p2Instants = 1)
                g.castAndAdvance(1, 1)
                withClue("two spells in total, but no single player cast two") {
                    g.isOnBattlefield("Call of the Full Moon") shouldBe true
                }
            }

            test("casting the Aura plus one other spell in a turn sacrifices it at the next upkeep") {
                val g = game(p1Instants = 1, attached = false)
                val bears = g.findPermanent("Grizzly Bears")!!
                g.castSpell(1, "Call of the Full Moon", bears).error shouldBe null
                g.resolveStack()
                g.isOnBattlefield("Call of the Full Moon") shouldBe true
                g.castAndAdvance(1, 0)
                withClue("the Aura's own casting counts toward the previous turn's two spells") {
                    g.isInGraveyard(1, "Call of the Full Moon") shouldBe true
                }
            }

            test("casting only the Aura keeps it") {
                val g = game(attached = false)
                val bears = g.findPermanent("Grizzly Bears")!!
                g.castSpell(1, "Call of the Full Moon", bears).error shouldBe null
                g.resolveStack()
                g.castAndAdvance(0, 0)
                g.isOnBattlefield("Call of the Full Moon") shouldBe true
            }
        }
    }
}
