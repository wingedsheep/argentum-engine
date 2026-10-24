package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Detention Vortex (STX #13).
 *
 *   Enchant nonland permanent
 *   Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 *   {3}: Destroy this Aura. Only your opponents may activate this ability and only as a sorcery.
 *
 * The {3} is the opponent's escape hatch, and "only as a sorcery" is timed for *them*: their own
 * main phase with an empty stack, never during the Aura controller's turn.
 */
class DetentionVortexScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Detention Vortex")!!.script.activatedAbilities[0].id
    }

    /** Player 1's Detention Vortex is on player 2's Llanowar Elves; player 2 has three Forests. */
    private fun board(activePlayer: Int) = scenario()
        .withPlayers("Warden", "Opponent")
        .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
        .withCardAttachedTo(1, "Detention Vortex", "Llanowar Elves")
        .withLandsOnBattlefield(2, "Forest", 3)
        .withLandsOnBattlefield(1, "Plains", 3)
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.vortexActivation(playerNumber: Int) =
        getLegalActions(playerNumber).map { it.action }.firstOrNull {
            it is ActivateAbility && it.abilityId == abilityId
        }

    init {
        context("Detention Vortex") {

            test("locks the enchanted permanent out of combat and its activated abilities, mana included") {
                val game = board(activePlayer = 2)
                val elves = game.findPermanent("Llanowar Elves")!!

                game.state.projectedState.cantAttack(elves) shouldBe true
                game.state.projectedState.cantBlock(elves) shouldBe true
                withClue("the Elves' mana ability is locked") {
                    game.getLegalActions(2).map { it.action }.any {
                        it is ActivateAbility && it.sourceId == elves
                    } shouldBe false
                }
            }

            test("an opponent destroys it for {3} in their own main phase") {
                val game = board(activePlayer = 2)

                val activation = game.vortexActivation(2)
                withClue("the opponent is offered the {3} ability on their turn") { activation shouldNotBe null }
                game.execute(activation!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Detention Vortex") shouldBe true
            }

            test("the opponent can't activate it during the Aura controller's turn") {
                val game = board(activePlayer = 1)
                val vortex = game.findPermanent("Detention Vortex")!!

                // Player 1 passes in their main phase, so player 2 holds priority on an empty stack.
                game.passPriority().error shouldBe null
                game.state.priorityPlayerId shouldBe game.player2Id

                withClue("not offered to the opponent outside their own main phase") {
                    game.vortexActivation(2) shouldBe null
                }
                game.execute(ActivateAbility(game.player2Id, vortex, abilityId)).error shouldNotBe null
            }

            test("its controller can't activate it") {
                val game = board(activePlayer = 1)
                val vortex = game.findPermanent("Detention Vortex")!!

                game.vortexActivation(1) shouldBe null
                game.execute(ActivateAbility(game.player1Id, vortex, abilityId)).error shouldNotBe null
            }
        }
    }
}
