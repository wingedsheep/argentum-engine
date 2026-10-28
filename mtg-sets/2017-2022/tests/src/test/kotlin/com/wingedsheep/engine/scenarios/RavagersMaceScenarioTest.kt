package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ravager's Mace (ZNR #235) — {1}{B}{R} Artifact — Equipment.
 *
 *   When this Equipment enters, attach it to target creature you control.
 *   Equipped creature gets +1/+0 for each creature in your party and has menace.
 *   Equip {2}{B}{R}
 *
 * The first static (continuously re-counted) reader of the party size: the bonus counts only the
 * Mace controller's Clerics/Rogues/Warriors/Wizards, at most one of each.
 */
class RavagersMaceScenarioTest : ScenarioTestBase() {

    init {
        context("Ravager's Mace") {

            test("enters attached and grants +1/+0 per creature in your party, plus menace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ravager's Mace")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Angelheart Protector")   // Cleric
                    .withCardOnBattlefield(1, "Ardent Electromancer")   // Wizard
                    .withCardOnBattlefield(1, "Cunning Geysermage")     // a second Wizard: not counted
                    .withCardOnBattlefield(2, "Dauntless Survivor")     // opponent's Warrior: not counted
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ravager's Mace").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                val mace = game.findPermanent("Ravager's Mace")!!
                withClue("the ETB trigger attaches the Mace to the Bears") {
                    game.state.getEntity(mace)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("party of two (Cleric + Wizard): +2/+0 and menace") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
                }
            }

            test("an empty party gives +0/+0 but still menace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ravager's Mace")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ravager's Mace").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
            }
        }
    }
}
