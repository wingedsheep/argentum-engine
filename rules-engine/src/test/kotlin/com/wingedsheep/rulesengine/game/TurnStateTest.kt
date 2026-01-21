package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.ecs.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.assertions.throwables.shouldThrow

class TurnStateTest : FunSpec({

    val player1 = EntityId.of("player1")
    val player2 = EntityId.of("player2")
    val playerOrder = listOf(player1, player2)

    context("creation") {
        test("newGame creates initial state") {
            val state = TurnState.newGame(playerOrder)

            state.turnNumber shouldBe 1
            state.activePlayer shouldBe player1
            state.priorityPlayer shouldBe player1
            state.phase shouldBe Phase.BEGINNING
            state.step shouldBe Step.UNTAP
            state.isFirstTurn.shouldBeTrue()
        }

        test("newGame with starting player") {
            val state = TurnState.newGame(playerOrder, player2)

            state.activePlayer shouldBe player2
            state.priorityPlayer shouldBe player2
        }

        test("throws when player order is empty") {
            shouldThrow<IllegalArgumentException> {
                TurnState.newGame(emptyList())
            }
        }

        test("throws when starting player not in order") {
            val player3 = EntityId.of("player3")
            shouldThrow<IllegalArgumentException> {
                TurnState.newGame(playerOrder, player3)
            }
        }
    }

    context("advanceStep") {
        test("advances through steps") {
            val state = TurnState.newGame(playerOrder)

            val afterUpkeep = state.advanceStep()
            afterUpkeep.step shouldBe Step.UPKEEP
            afterUpkeep.phase shouldBe Phase.BEGINNING

            val afterDraw = afterUpkeep.advanceStep()
            afterDraw.step shouldBe Step.DRAW

            val afterMain1 = afterDraw.advanceStep()
            afterMain1.step shouldBe Step.PRECOMBAT_MAIN
            afterMain1.phase shouldBe Phase.PRECOMBAT_MAIN
        }

        test("wraps to next turn after cleanup") {
            var state = TurnState.newGame(playerOrder)

            // Advance through entire turn (13 steps including first strike damage step)
            repeat(13) {
                state = state.advanceStep()
            }

            // Should be in turn 2
            state.turnNumber shouldBe 2
            state.activePlayer shouldBe player2
            state.step shouldBe Step.UNTAP
            state.isFirstTurn.shouldBeFalse()
        }
    }

    // Note: advanceToNextTurn, advanceToPhase, advanceToStep are private
    // Turn transitions are tested via advanceStep() in the "wraps to next turn after cleanup" test above

    context("priority") {
        test("passPriority moves to next player") {
            val state = TurnState.newGame(playerOrder)
            val passed = state.passPriority()

            passed.priorityPlayer shouldBe player2
            passed.activePlayer shouldBe player1 // Active player unchanged
        }

        test("passPriority wraps around") {
            val state = TurnState.newGame(playerOrder).passPriority()
            val passed = state.passPriority()

            passed.priorityPlayer shouldBe player1
        }

        test("resetPriorityToActivePlayer") {
            val state = TurnState.newGame(playerOrder).passPriority()
            val reset = state.resetPriorityToActivePlayer()

            reset.priorityPlayer shouldBe player1
        }

        test("priorityPassedByAllPlayers") {
            val state = TurnState.newGame(playerOrder)

            // Player 1 has priority, hasn't passed yet
            state.priorityPassedByAllPlayers(player1).shouldBeTrue()

            // Player 1 passes, now player 2 has priority
            val afterPass = state.passPriority()
            afterPass.priorityPassedByAllPlayers(player1).shouldBeFalse()

            // Player 2 passes, back to player 1
            val fullRound = afterPass.passPriority()
            fullRound.priorityPassedByAllPlayers(player1).shouldBeTrue()
        }
    }

    context("canPlaySorcerySpeed") {
        test("true during main phase when active player has priority") {
            // Advance UNTAP -> UPKEEP -> DRAW -> PRECOMBAT_MAIN (3 steps)
            var state = TurnState.newGame(playerOrder)
            repeat(3) { state = state.advanceStep() }
            state.step shouldBe Step.PRECOMBAT_MAIN

            state.canPlaySorcerySpeed.shouldBeTrue()
        }

        test("false during main phase when non-active player has priority") {
            // Advance UNTAP -> UPKEEP -> DRAW -> PRECOMBAT_MAIN (3 steps)
            var state = TurnState.newGame(playerOrder)
            repeat(3) { state = state.advanceStep() }
            state = state.passPriority()

            state.canPlaySorcerySpeed.shouldBeFalse()
        }

        test("false during combat") {
            // Advance UNTAP -> UPKEEP -> DRAW -> PRECOMBAT_MAIN -> BEGIN_COMBAT -> DECLARE_ATTACKERS (5 steps)
            var state = TurnState.newGame(playerOrder)
            repeat(5) { state = state.advanceStep() }
            state.step shouldBe Step.DECLARE_ATTACKERS

            state.canPlaySorcerySpeed.shouldBeFalse()
        }
    }
})
