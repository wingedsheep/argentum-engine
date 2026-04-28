package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Tributary Vaulter.
 *
 * Tributary Vaulter — {2}{W} 1/3 Creature — Merfolk Warrior
 *   Flying
 *   Whenever this creature becomes tapped, another target Merfolk you
 *   control gets +2/+0 until end of turn.
 *
 * Verifies the trigger fires when the Vaulter is tapped by an external
 * source (Icy Manipulator), not just when it taps itself by attacking.
 */
class TributaryVaulterScenarioTest : ScenarioTestBase() {

    init {
        context("Tributary Vaulter — becomes tapped trigger") {
            test("fires when tapped by another permanent's ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tributary Vaulter")
                    .withCardOnBattlefield(1, "Eclipsed Merrow") // 2/3 Merfolk Scout
                    .withCardOnBattlefield(1, "Icy Manipulator")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vaulter = game.findPermanent("Tributary Vaulter")!!
                val merrow = game.findPermanent("Eclipsed Merrow")!!
                val icy = game.findPermanent("Icy Manipulator")!!

                val icyDef = cardRegistry.getCard("Icy Manipulator")!!
                val tapAbility = icyDef.script.activatedAbilities[0]

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = icy,
                        abilityId = tapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(vaulter))
                    )
                )
                withClue("Icy Manipulator should activate (error=${activateResult.error})") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Vaulter's becomes-tapped trigger should pause for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                game.selectTargets(listOf(merrow))
                game.resolveStack()

                val merrowInfo = game.getClientState(1).cards[merrow]!!
                withClue("Eclipsed Merrow should be 4/3 (base 2/3 + 2/0)") {
                    merrowInfo.power shouldBe 4
                    merrowInfo.toughness shouldBe 3
                }
            }

            test("fires when tapped by an opponent's permanent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tributary Vaulter")
                    .withCardOnBattlefield(1, "Eclipsed Merrow")
                    .withCardOnBattlefield(2, "Icy Manipulator")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vaulter = game.findPermanent("Tributary Vaulter")!!
                val merrow = game.findPermanent("Eclipsed Merrow")!!
                val icy = game.findPermanent("Icy Manipulator")!!

                val icyDef = cardRegistry.getCard("Icy Manipulator")!!
                val tapAbility = icyDef.script.activatedAbilities[0]

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = icy,
                        abilityId = tapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(vaulter))
                    )
                )
                withClue("Opponent's Icy Manipulator should activate (error=${activateResult.error})") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Vaulter's becomes-tapped trigger should pause for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                game.selectTargets(listOf(merrow))
                game.resolveStack()

                val merrowInfo = game.getClientState(1).cards[merrow]!!
                withClue("Eclipsed Merrow should be 4/3 (base 2/3 + 2/0)") {
                    merrowInfo.power shouldBe 4
                    merrowInfo.toughness shouldBe 3
                }
            }

            test("fires when tapped by attacking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tributary Vaulter")
                    .withCardOnBattlefield(1, "Eclipsed Merrow")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val merrow = game.findPermanent("Eclipsed Merrow")!!
                game.declareAttackers(mapOf("Tributary Vaulter" to 2))

                withClue("Vaulter's becomes-tapped trigger should pause for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                game.selectTargets(listOf(merrow))
                game.resolveStack()

                val merrowInfo = game.getClientState(1).cards[merrow]!!
                withClue("Eclipsed Merrow should be 4/3") {
                    merrowInfo.power shouldBe 4
                    merrowInfo.toughness shouldBe 3
                }
            }
        }
    }
}
