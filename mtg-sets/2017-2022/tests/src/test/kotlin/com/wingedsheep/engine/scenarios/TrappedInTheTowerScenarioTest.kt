package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Trapped in the Tower (ELD #33) — {1}{W} Enchantment — Aura.
 *
 *   Enchant creature without flying
 *   Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * The first "Enchant … without <keyword>" Aura: the restriction gates casting, and the enchant
 * state-based action re-reads it against projected state, so a host that gains flying sheds it.
 */
class TrappedInTheTowerScenarioTest : ScenarioTestBase() {

    init {
        context("Trapped in the Tower") {

            test("can't enchant a creature with flying, can enchant one without") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Trapped in the Tower")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("A flier is not a legal host") {
                    game.castSpell(1, "Trapped in the Tower", drake).error shouldNotBe null
                }
                withClue("A creature without flying is a legal host") {
                    game.castSpell(1, "Trapped in the Tower", bears).error shouldBe null
                }
                game.resolveStack()

                withClue("Trapped in the Tower is attached to Grizzly Bears") {
                    game.isOnBattlefield("Trapped in the Tower") shouldBe true
                    game.state.projectedState.cantAttack(bears) shouldBe true
                    game.state.projectedState.cantBlock(bears) shouldBe true
                }
            }

            test("enchanted creature can't attack") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Trapped in the Tower", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("The enchanted creature can't be declared as an attacker") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldNotBe null
                }
            }

            test("if the enchanted creature gains flying, the Aura is put into the graveyard") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Trapped in the Tower", "Grizzly Bears")
                    .withCardInHand(2, "Jump")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Jump", bears).error shouldBe null
                game.resolveStack()

                withClue("The Aura's enchant restriction no longer holds, so it goes to the graveyard") {
                    game.isOnBattlefield("Trapped in the Tower") shouldBe false
                    game.isInGraveyard(1, "Trapped in the Tower") shouldBe true
                }
                withClue("Freed from the tower, the creature can attack again") {
                    game.state.projectedState.cantAttack(bears) shouldBe false
                }
            }
        }
    }
}
