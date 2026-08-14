package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Orcrist, Goblin-cleaver (HOB #177) — {3} Legendary Artifact — Equipment
 *
 * Equipped creature gets +2/+2 and has trample.
 * Whenever equipped creature deals combat damage to a player, choose a creature type. Create a
 * Treasure token for each creature you control of that type.
 * Equip {3}
 *
 * The composition under test is the second ability: a resolution-time creature-type choice feeding a
 * *dynamic* token count. The risk it covers is the count reading the wrong thing — a fixed 1, the
 * whole board, or a type chosen before the board was re-read.
 */
class OrcristGoblinCleaverScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    /** Push through combat until the Equipment's creature-type choice surfaces. */
    private fun advanceToTypeChoice(game: TestGame): ChooseOptionDecision {
        var blockersDeclared = false
        var guard = 0
        while (guard++ < 60) {
            val pending = game.getPendingDecision()
            if (pending is ChooseOptionDecision) return pending
            when {
                pending is CombatResolutionDecision -> game.submitDefaultCombatDamage()
                pending is AssignDamageDecision -> game.submitDefaultDamageAssignment()
                pending != null -> error("unexpected pending decision: $pending")
                game.state.step == Step.DECLARE_BLOCKERS && !blockersDeclared -> {
                    blockersDeclared = true
                    game.declareNoBlockers()
                }
                else -> game.passPriority()
            }
        }
        error("no creature-type choice surfaced; last was ${game.getPendingDecision()}")
    }

    private fun treasureCount(game: TestGame): Int {
        val projected = projector.project(game.state)
        return game.state.getBattlefield(game.player1Id).count { projected.hasSubtype(it, "Treasure") }
    }

    init {
        context("Orcrist, Goblin-cleaver") {

            test("the static half grants +2/+2 and trample to the equipped creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardAttachedTo(1, "Orcrist, Goblin-cleaver", "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Ordinary Bear")!!
                val projected = projector.project(game.state)

                withClue("Ordinary Bear is a 4/5, so equipping makes it 6/7") {
                    projected.getPower(bear) shouldBe 6
                    projected.getToughness(bear) shouldBe 7
                }
                projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
            }

            test("combat damage mints one Treasure per creature you control of the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardOnBattlefield(1, "Large Bear")
                    .withCardOnBattlefield(1, "Goblin-town Flunkies")
                    .withCardAttachedTo(1, "Orcrist, Goblin-cleaver", "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ordinary Bear" to 2)).error shouldBe null

                val decision = advanceToTypeChoice(game)
                val bearIndex = decision.options.indexOf("Bear")
                withClue("Bear must be offered as a creature type") { (bearIndex >= 0) shouldBe true }
                game.submitDecision(OptionChosenResponse(decision.id, bearIndex))
                game.resolveStack()

                withClue("Two Bears on the battlefield -> two Treasures, not one and not three") {
                    treasureCount(game) shouldBe 2
                }
            }

            test("naming a type you control none of makes no Treasures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardAttachedTo(1, "Orcrist, Goblin-cleaver", "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ordinary Bear" to 2)).error shouldBe null

                val decision = advanceToTypeChoice(game)
                val dragonIndex = decision.options.indexOf("Dragon")
                game.submitDecision(OptionChosenResponse(decision.id, dragonIndex))
                game.resolveStack()

                withClue("No Dragons controlled -> zero Treasures, and no crash on an empty count") {
                    treasureCount(game) shouldBe 0
                }
            }
        }
    }
}
