package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Azorius Justiciar (RTR #6) — {2}{W}{W} Creature — Human Wizard 2/2.
 *
 *   When this creature enters, detain up to two target creatures your opponents control.
 *
 * Both detained creatures can't attack or block and can't activate abilities through the
 * opponent's turn, and are free again once the Justiciar's controller starts their next turn.
 */
class AzoriusJusticiarScenarioTest : ScenarioTestBase() {

    private val elvesManaAbilityId =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id

    private fun TestGame.canActivate(playerNumber: Int, source: EntityId): Boolean =
        getLegalActions(playerNumber).any {
            val a = it.action
            a is ActivateAbility && a.sourceId == source && a.abilityId == elvesManaAbilityId
        }

    init {
        context("Azorius Justiciar") {

            test("detains two creatures until its controller's next turn") {
                var builder = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Azorius Justiciar")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) {
                    builder = builder.withCardInLibrary(1, "Plains").withCardInLibrary(2, "Forest")
                }
                val game = builder.build()
                val elves = game.findPermanents("Llanowar Elves")
                elves.size shouldBe 2

                game.castSpell(1, "Azorius Justiciar").error shouldBe null
                game.resolveStack()
                withClue("Enter trigger should ask for up to two targets") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(elves).error shouldBe null
                game.resolveStack()

                // Opponent's turn: both elves are detained.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                for (elf in elves) {
                    withClue("Detained creature can't attack on the opponent's turn") {
                        game.state.projectedState.cantAttack(elf) shouldBe true
                    }
                    withClue("Detained creature can't block on the opponent's turn") {
                        game.state.projectedState.cantBlock(elf) shouldBe true
                    }
                    withClue("Detained creature's mana ability can't be activated") {
                        game.canActivate(2, elf) shouldBe false
                    }
                }

                // Controller's next turn: the detention has ended.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                // P1 passes so the opponent holds priority in P1's main phase.
                game.execute(PassPriority(game.player1Id)).error shouldBe null
                for (elf in elves) {
                    withClue("Creature is free to attack again") {
                        game.state.projectedState.cantAttack(elf) shouldBe false
                    }
                    withClue("Creature is free to block again") {
                        game.state.projectedState.cantBlock(elf) shouldBe false
                    }
                    withClue("Creature's mana ability can be activated again") {
                        game.canActivate(2, elf) shouldBe true
                    }
                }
            }
        }
    }
}
