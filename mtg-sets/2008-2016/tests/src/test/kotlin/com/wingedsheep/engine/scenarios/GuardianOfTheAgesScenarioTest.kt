package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Guardian of the Ages (M14 #211) — {7} Artifact Creature — Golem 7/7.
 *
 * "Defender. When a creature attacks you or a planeswalker you control, if this creature has
 * defender, it loses defender and gains trample."
 *
 * Pins the intervening "if" across several simultaneous attack triggers, and that the keyword
 * changes are permanent rather than until end of turn.
 */
class GuardianOfTheAgesScenarioTest : ScenarioTestBase() {

    private val name = "Guardian of the Ages"

    private fun opponentsTurn(): TestGame =
        scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    init {
        test("an opponent's attack makes it lose defender and gain trample, and that lasts into the next turn") {
            val game = opponentsTurn()
            val guardian = game.findPermanent(name)!!

            game.state.projectedState.hasKeyword(guardian, Keyword.DEFENDER) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Savannah Lions" to 1)).error shouldBe null
            game.resolveStack()

            withClue("after the triggers resolve it has lost defender and gained trample") {
                val projected = game.state.projectedState
                projected.hasKeyword(guardian, Keyword.DEFENDER) shouldBe false
                projected.hasKeyword(guardian, Keyword.TRAMPLE) shouldBe true
            }

            // Into its controller's own turn: the change doesn't wear off at end of turn.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("still no defender and still trample on the next turn") {
                val projected = game.state.projectedState
                projected.hasKeyword(guardian, Keyword.DEFENDER) shouldBe false
                projected.hasKeyword(guardian, Keyword.TRAMPLE) shouldBe true
            }
        }

        test("once it has lost defender, a later attack doesn't trigger it") {
            val game = opponentsTurn()
            val guardian = game.findPermanent(name)!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(guardian, Keyword.DEFENDER) shouldBe false

            // Two turns on, the opponent attacks again: with no defender there is no trigger.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // P1's turn
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // P2's turn
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            withClue("no attack trigger goes on the stack") {
                game.state.stack.isEmpty() shouldBe true
            }
            val projected = game.state.projectedState
            projected.hasKeyword(guardian, Keyword.DEFENDER) shouldBe false
            projected.hasKeyword(guardian, Keyword.TRAMPLE) shouldBe true
        }
    }
}
