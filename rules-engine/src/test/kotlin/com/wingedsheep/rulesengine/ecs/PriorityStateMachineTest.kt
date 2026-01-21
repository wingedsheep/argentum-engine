package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PriorityStateMachineTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    // Helper to advance step by step to a target step
    fun GameState.advanceToStep(targetStep: Step): GameState {
        var state = this
        var iterations = 0
        while (state.turnState.step != targetStep && iterations < 20) {
            state = state.copy(turnState = state.turnState.advanceStep())
            iterations++
        }
        require(state.turnState.step == targetStep) { "Failed to reach step $targetStep" }
        return state
    }

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    context("consecutivePasses counter") {
        test("starts at zero") {
            val state = newGame()
            state.turnState.consecutivePasses shouldBe 0
        }

        test("increments when player passes priority") {
            val state = newGame().advanceToStep(Step.PRECOMBAT_MAIN)

            val result = GameEngine.executeAction(state, PassPriority(player1Id))
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state
            newState.turnState.consecutivePasses shouldBe 1
        }

        test("increments for each player pass") {
            var state = newGame().advanceToStep(Step.PRECOMBAT_MAIN)

            // Player 1 passes
            val result1 = GameEngine.executeAction(state, PassPriority(player1Id))
            state = (result1 as GameActionResult.Success).state
            state.turnState.consecutivePasses shouldBe 1
            state.turnState.priorityPlayer shouldBe player2Id

            // Player 2 passes
            val result2 = GameEngine.executeAction(state, PassPriority(player2Id))
            state = (result2 as GameActionResult.Success).state
            state.turnState.consecutivePasses shouldBe 2
        }

        test("resets when step advances") {
            val state = newGame()
            val stateWithPasses = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 2)
            )

            val advanced = stateWithPasses.copy(
                turnState = stateWithPasses.turnState.advanceStep()
            )

            advanced.turnState.consecutivePasses shouldBe 0
        }

        test("resets when turn advances") {
            var state = newGame()
            // Advance through entire turn (13 steps) to reach turn 2
            repeat(13) { state = state.copy(turnState = state.turnState.advanceStep()) }

            state.turnState.turnNumber shouldBe 2
            state.turnState.consecutivePasses shouldBe 0
        }
    }

    context("allPlayersPassed") {
        test("false when no one has passed") {
            val state = newGame()
            state.turnState.allPlayersPassed().shouldBeFalse()
        }

        test("false when only one player has passed in 2-player game") {
            val state = newGame()
            val onePass = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 1)
            )
            onePass.turnState.allPlayersPassed().shouldBeFalse()
        }

        test("true when all players have passed") {
            val state = newGame()
            val allPassed = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 2)
            )
            allPassed.turnState.allPlayersPassed().shouldBeTrue()
        }
    }

    context("executePlayerAction resets passes") {
        test("resets consecutivePasses after action") {
            var state = newGame().advanceToStep(Step.PRECOMBAT_MAIN)

            // Set up some passes
            state = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 1)
            )

            // Execute a player action (e.g., gain life)
            val result = GameEngine.executePlayerAction(state, GainLife(player1Id, 5))
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state
            newState.turnState.consecutivePasses shouldBe 0
        }
    }

    context("resolvePassedPriority") {
        test("advances step when stack is empty") {
            var state = newGame().advanceToStep(Step.UPKEEP)
            state = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 2)
            )

            val resolved = GameEngine.resolvePassedPriority(state)

            resolved.turnState.step shouldBe Step.DRAW
            resolved.turnState.consecutivePasses shouldBe 0
        }

        test("resolves top of stack when stack not empty") {
            var state = newGame().advanceToStep(Step.PRECOMBAT_MAIN)

            // Create a card and put it on the stack
            val cardId = EntityId.generate()
            val (_, stateWithCard) = state.createEntity(
                cardId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                SpellOnStackComponent(player1Id)
            )
            val stateWithStack = stateWithCard.addToZone(cardId, ZoneId.STACK)
            val stateWithPasses = stateWithStack.copy(
                turnState = stateWithStack.turnState.copy(consecutivePasses = 2)
            )

            stateWithPasses.getStack().size shouldBe 1

            val resolved = GameEngine.resolvePassedPriority(stateWithPasses)

            // Stack should be empty (resolved to battlefield for permanent)
            resolved.getStack().isEmpty().shouldBeTrue()
            resolved.turnState.consecutivePasses shouldBe 0
            resolved.turnState.priorityPlayer shouldBe player1Id // Reset to active player
        }
    }

    context("processPriority") {
        test("returns PriorityGranted when game not over") {
            val state = newGame().advanceToStep(Step.PRECOMBAT_MAIN)

            val result = GameEngine.processPriority(state)
            result.shouldBeInstanceOf<PriorityResult.PriorityGranted>()

            val granted = result as PriorityResult.PriorityGranted
            granted.priorityPlayer shouldBe player1Id
        }

        test("returns GameOver when game is over") {
            val state = newGame()
            val gameOver = state.endGame(player1Id)

            val result = GameEngine.processPriority(gameOver)
            result.shouldBeInstanceOf<PriorityResult.GameOver>()

            val over = result as PriorityResult.GameOver
            over.winner shouldBe player1Id
        }

        test("checks SBAs before granting priority") {
            var state = newGame()

            // Set player 2 to 0 life
            state = state.updateEntity(player2Id) { c ->
                c.with(LifeComponent(0))
            }

            val result = GameEngine.processPriority(state)
            result.shouldBeInstanceOf<PriorityResult.GameOver>()

            val over = result as PriorityResult.GameOver
            over.winner shouldBe player1Id
        }
    }

    context("priority flow integration") {
        test("full priority pass cycle advances step") {
            var state = newGame().advanceToStep(Step.UPKEEP)
            val initialStep = state.turnState.step

            // Player 1 passes
            val result1 = GameEngine.executeAction(state, PassPriority(player1Id))
            state = (result1 as GameActionResult.Success).state

            // Player 2 passes
            val result2 = GameEngine.executeAction(state, PassPriority(player2Id))
            state = (result2 as GameActionResult.Success).state

            // Both have passed, so resolvePassedPriority should advance
            state.turnState.allPlayersPassed().shouldBeTrue()

            val resolved = GameEngine.resolvePassedPriority(state)
            resolved.turnState.step shouldBe Step.DRAW
        }
    }
})
