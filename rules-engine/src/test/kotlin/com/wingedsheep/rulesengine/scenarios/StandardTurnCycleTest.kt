package com.wingedsheep.rulesengine.scenarios

import com.wingedsheep.rulesengine.card.CardDefinition
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

/**
 * Scenario 1.1: The Standard Turn Cycle ("Happy Path")
 * Target System: Phase State Machine / Land Play Logic
 * Complexity: Basic
 *
 * Tests the standard flow of a turn where no responses occur.
 * Validates automatic transition logic and basic resource accumulation.
 *
 * Setup:
 * - Active Player (AP): Player A
 * - Hand: Mountain
 * - Current State: Beginning of Turn
 *
 * Action Sequence:
 * 1. Untap Step (Engine untaps permanents)
 * 2. Upkeep Step: AP passes, NAP passes
 * 3. Draw Step: AP draws (Turn-Based Action), AP passes, NAP passes
 * 4. Precombat Main Phase: AP plays Mountain (Special Action), AP passes, NAP passes
 * 5. Combat Phase: All steps passed through without attackers
 * 6. Postcombat Main Phase: Players pass
 * 7. End Step: Players pass
 * 8. Cleanup Step
 *
 * Expected Outcomes:
 * - Land Drop: Engine acknowledges playing land without using the stack
 * - Step Sequencing: Engine visits every step in Combat Phase
 * - Turn End: Active player token shifts to Player B after Cleanup
 *
 * Rules Analysis: CR 500.1 defines the rigid five-phase structure.
 */
class StandardTurnCycleTest : FunSpec({

    val player1Id = com.wingedsheep.rulesengine.ecs.EntityId.Companion.of("player1")  // Alice (Active Player)
    val player2Id = com.wingedsheep.rulesengine.ecs.EntityId.Companion.of("player2")  // Bob (Non-Active Player)

    fun newGame(): com.wingedsheep.rulesengine.ecs.GameState = com.wingedsheep.rulesengine.ecs.GameState.Companion.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    val mountainDef = CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN)

    /**
     * Helper to put a card in a player's hand for testing.
     */
    fun com.wingedsheep.rulesengine.ecs.GameState.putCardInHand(cardDef: CardDefinition, playerId: com.wingedsheep.rulesengine.ecs.EntityId): Pair<com.wingedsheep.rulesengine.ecs.EntityId, com.wingedsheep.rulesengine.ecs.GameState> {
        val (cardId, state1) = createEntity(
            com.wingedsheep.rulesengine.ecs.EntityId.Companion.generate(),
            CardComponent(cardDef, playerId),
            ControllerComponent(playerId)
        )
        val state2 = state1.addToZone(cardId, com.wingedsheep.rulesengine.ecs.ZoneId.Companion.hand(playerId))
        return cardId to state2
    }

    /**
     * Helper to put cards in a player's library for testing.
     */
    fun com.wingedsheep.rulesengine.ecs.GameState.putCardsInLibrary(cardDef: CardDefinition, playerId: com.wingedsheep.rulesengine.ecs.EntityId, count: Int): com.wingedsheep.rulesengine.ecs.GameState {
        var state = this
        repeat(count) {
            val (cardId, newState) = state.createEntity(
                com.wingedsheep.rulesengine.ecs.EntityId.Companion.generate(),
                CardComponent(cardDef, playerId),
                ControllerComponent(playerId)
            )
            state = newState.addToZone(cardId, com.wingedsheep.rulesengine.ecs.ZoneId.Companion.library(playerId))
        }
        return state
    }

    /**
     * Helper to have both players pass priority and let the engine advance.
     * This is the proper way to test the engine - priority passing drives state transitions.
     *
     * Note: The rules-engine does NOT auto-advance through non-priority steps (UNTAP, CLEANUP).
     * Auto-advancement is handled by the game-server. Tests must manually advance through
     * non-priority steps using advanceFromUntap() or advanceFromCleanup().
     * However, SBAs are run after resolution/step advancement.
     */
    fun passAndAdvance(state: com.wingedsheep.rulesengine.ecs.GameState): com.wingedsheep.rulesengine.ecs.GameState {
        // Active player passes priority
        val result1 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PassPriority(state.turnState.priorityPlayer))
        var currentState = (result1 as GameActionResult.Success).state

        // Non-active player passes priority
        val result2 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(currentState, PassPriority(currentState.turnState.priorityPlayer))
        currentState = (result2 as GameActionResult.Success).state

        // All players have passed, engine resolves (advances step or resolves stack)
        currentState.turnState.allPlayersPassed().shouldBeTrue()
        currentState = com.wingedsheep.rulesengine.ecs.GameEngine.resolvePassedPriority(currentState)

        // Run SBAs (e.g., creatures die from lethal damage)
        // Note: processPriority no longer auto-advances, it just runs SBAs
        val priorityResult = com.wingedsheep.rulesengine.ecs.GameEngine.processPriority(currentState)
        return when (priorityResult) {
            is com.wingedsheep.rulesengine.ecs.PriorityResult.PriorityGranted -> priorityResult.state
            is com.wingedsheep.rulesengine.ecs.PriorityResult.GameOver -> priorityResult.state
        }
    }

    /**
     * Advances from UNTAP step (which has no priority) to UPKEEP.
     */
    fun advanceFromUntap(state: com.wingedsheep.rulesengine.ecs.GameState): com.wingedsheep.rulesengine.ecs.GameState {
        state.turnState.step shouldBe Step.UNTAP
        return state.copy(turnState = state.turnState.advanceStep())
    }

    /**
     * Advances from CLEANUP step (which has no priority) to next turn's UNTAP.
     */
    fun advanceFromCleanup(state: com.wingedsheep.rulesengine.ecs.GameState): com.wingedsheep.rulesengine.ecs.GameState {
        state.turnState.step shouldBe Step.CLEANUP
        return state.copy(turnState = state.turnState.advanceStep())
    }

    context("Scenario 1.1: Standard Turn Cycle - Happy Path") {

        test("game starts at untap step with player 1 as active player") {
            val state = newGame()

            state.turnState.turnNumber shouldBe 1
            state.turnState.activePlayer shouldBe player1Id
            state.turnState.phase shouldBe Phase.BEGINNING
            state.turnState.step shouldBe Step.UNTAP
            state.turnState.isFirstTurn.shouldBeTrue()
        }

        test("untap step has no priority - engine advances directly to upkeep") {
            var state = newGame()

            // Untap step does not grant priority (CR 502.3)
            Step.UNTAP.hasPriority.shouldBeFalse()
            state.turnState.step shouldBe Step.UNTAP

            // Advance from untap (no priority, direct advance)
            state = advanceFromUntap(state)

            state.turnState.step shouldBe Step.UPKEEP
            state.turnState.phase shouldBe Phase.BEGINNING
            state.turnState.priorityPlayer shouldBe player1Id
        }

        test("upkeep step - both players pass priority, engine advances to draw step") {
            var state = newGame()

            // Advance from UNTAP (no priority)
            state = advanceFromUntap(state)
            state.turnState.step shouldBe Step.UPKEEP
            Step.UPKEEP.hasPriority.shouldBeTrue()

            // Both players pass priority
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.DRAW
            state.turnState.phase shouldBe Phase.BEGINNING
        }

        test("draw step - active player draws a card as turn-based action") {
            var state = newGame()
            // Put a card in player1's library to draw
            state = state.putCardsInLibrary(mountainDef, player1Id, 5)

            // Advance through UNTAP and UPKEEP
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state.turnState.step shouldBe Step.DRAW

            val initialLibrarySize = state.getLibrary(player1Id).size
            val initialHandSize = state.getHand(player1Id).size

            // Execute turn-based draw action
            val drawResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DrawCard(player1Id, 1))
            drawResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (drawResult as GameActionResult.Success).state

            // Verify card was drawn
            state.getLibrary(player1Id).size shouldBe initialLibrarySize - 1
            state.getHand(player1Id).size shouldBe initialHandSize + 1

            // Both players pass priority
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.PRECOMBAT_MAIN
            state.turnState.phase shouldBe Phase.PRECOMBAT_MAIN
        }

        test("precombat main phase - active player plays land as special action (no stack)") {
            var state = newGame()
            // Put Mountain in player1's hand
            val (mountainId, stateWithMountain) = state.putCardInHand(mountainDef, player1Id)
            state = stateWithMountain

            // Advance through UNTAP, UPKEEP, DRAW to PRECOMBAT_MAIN
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state.turnState.step shouldBe Step.PRECOMBAT_MAIN

            // Verify main phase properties
            Step.PRECOMBAT_MAIN.isMainPhase.shouldBeTrue()
            Step.PRECOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()
            state.turnState.canPlaySorcerySpeed.shouldBeTrue()

            // Verify land can be played
            state.canPlayLand(player1Id).shouldBeTrue()
            state.getLandsPlayed(player1Id) shouldBe 0

            // Play the Mountain - special action, doesn't use stack
            val playLandResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountainId, player1Id))
            playLandResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (playLandResult as GameActionResult.Success).state

            // Verify events include LandPlayed
            playLandResult.events.any { it is GameActionEvent.LandPlayed }.shouldBeTrue()

            // Verify land is on battlefield
            state.isOnBattlefield(mountainId).shouldBeTrue()
            state.getHand(player1Id).contains(mountainId).shouldBeFalse()

            // Verify lands played counter incremented
            state.getLandsPlayed(player1Id) shouldBe 1
            state.canPlayLand(player1Id).shouldBeFalse()

            // Stack should still be empty (land play doesn't use stack)
            state.getStack().isEmpty().shouldBeTrue()

            // Both players pass
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.BEGIN_COMBAT
            state.turnState.phase shouldBe Phase.COMBAT
        }

        test("combat phase - all steps are visited even without attackers") {
            var state = newGame()

            // Advance to BEGIN_COMBAT through proper priority passing
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state = passAndAdvance(state) // PRECOMBAT_MAIN -> BEGIN_COMBAT
            state.turnState.step shouldBe Step.BEGIN_COMBAT

            val combatSteps = listOf(
                Step.BEGIN_COMBAT,
                Step.DECLARE_ATTACKERS,
                Step.DECLARE_BLOCKERS,
                Step.FIRST_STRIKE_COMBAT_DAMAGE,
                Step.COMBAT_DAMAGE,
                Step.END_COMBAT
            )

            // Verify all combat steps exist and are visited in order
            for (expectedStep in combatSteps) {
                state.turnState.step shouldBe expectedStep
                state.turnState.phase shouldBe Phase.COMBAT
                state = passAndAdvance(state)
            }

            // After combat ends, we should be in postcombat main
            state.turnState.step shouldBe Step.POSTCOMBAT_MAIN
            state.turnState.phase shouldBe Phase.POSTCOMBAT_MAIN
        }

        test("postcombat main phase - players pass") {
            var state = newGame()

            // Advance to POSTCOMBAT_MAIN through the full turn
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state = passAndAdvance(state) // PRECOMBAT_MAIN -> BEGIN_COMBAT
            state = passAndAdvance(state) // BEGIN_COMBAT -> DECLARE_ATTACKERS
            state = passAndAdvance(state) // DECLARE_ATTACKERS -> DECLARE_BLOCKERS
            state = passAndAdvance(state) // DECLARE_BLOCKERS -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // FIRST_STRIKE_COMBAT_DAMAGE -> COMBAT_DAMAGE
            state = passAndAdvance(state) // COMBAT_DAMAGE -> END_COMBAT
            state = passAndAdvance(state) // END_COMBAT -> POSTCOMBAT_MAIN

            state.turnState.step shouldBe Step.POSTCOMBAT_MAIN
            Step.POSTCOMBAT_MAIN.isMainPhase.shouldBeTrue()
            Step.POSTCOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()

            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.END
            state.turnState.phase shouldBe Phase.ENDING
        }

        test("end step - players pass and game advances to cleanup then next turn") {
            var state = newGame()

            // Advance to END step through the full turn
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state = passAndAdvance(state) // PRECOMBAT_MAIN -> BEGIN_COMBAT
            state = passAndAdvance(state) // BEGIN_COMBAT -> DECLARE_ATTACKERS
            state = passAndAdvance(state) // DECLARE_ATTACKERS -> DECLARE_BLOCKERS
            state = passAndAdvance(state) // DECLARE_BLOCKERS -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // FIRST_STRIKE_COMBAT_DAMAGE -> COMBAT_DAMAGE
            state = passAndAdvance(state) // COMBAT_DAMAGE -> END_COMBAT
            state = passAndAdvance(state) // END_COMBAT -> POSTCOMBAT_MAIN
            state = passAndAdvance(state) // POSTCOMBAT_MAIN -> END

            state.turnState.step shouldBe Step.END
            Step.END.hasPriority.shouldBeTrue()

            // Passing at END advances to CLEANUP (no priority step)
            state = passAndAdvance(state) // END -> CLEANUP
            state.turnState.step shouldBe Step.CLEANUP
            state.turnState.turnNumber shouldBe 1 // Still turn 1 during cleanup

            // Manually advance through CLEANUP to next turn's UNTAP, then to UPKEEP
            state = advanceFromCleanup(state) // CLEANUP -> turn 2 UNTAP
            state = advanceFromUntap(state) // UNTAP -> UPKEEP

            // Verify turn advanced
            state.turnState.turnNumber shouldBe 2
            state.turnState.activePlayer shouldBe player2Id
            state.turnState.priorityPlayer shouldBe player2Id
            state.turnState.phase shouldBe Phase.BEGINNING
            state.turnState.step shouldBe Step.UPKEEP
        }

        test("cleanup step has no priority") {
            // Cleanup step does not grant priority normally (CR 514.3)
            Step.CLEANUP.hasPriority.shouldBeFalse()

            // The rules-engine allows step-by-step testing by not auto-advancing.
            // The game-server handles auto-advancement through non-priority steps.
            // Here we can directly test cleanup step behavior.
        }

        test("full turn cycle - complete happy path") {
            var state = newGame()

            // Setup: Put Mountain in hand, cards in library for drawing
            val (mountainId, stateWithMountain) = state.putCardInHand(mountainDef, player1Id)
            state = stateWithMountain.putCardsInLibrary(mountainDef, player1Id, 5)
                .putCardsInLibrary(mountainDef, player2Id, 5)

            // Track visited steps for verification
            val visitedSteps = mutableListOf<Step>()

            // === TURN 1: Player 1 (Alice) ===

            // 1. UNTAP STEP (no priority)
            state.turnState.step shouldBe Step.UNTAP
            visitedSteps.add(state.turnState.step)
            state = advanceFromUntap(state)

            // 2. UPKEEP STEP
            state.turnState.step shouldBe Step.UPKEEP
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            // 3. DRAW STEP - draw a card
            state.turnState.step shouldBe Step.DRAW
            visitedSteps.add(state.turnState.step)
            val drawResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DrawCard(player1Id, 1))
            state = (drawResult as GameActionResult.Success).state
            state = passAndAdvance(state)

            // 4. PRECOMBAT MAIN PHASE - play Mountain
            state.turnState.step shouldBe Step.PRECOMBAT_MAIN
            visitedSteps.add(state.turnState.step)
            val playLandResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountainId, player1Id))
            state = (playLandResult as GameActionResult.Success).state
            state.isOnBattlefield(mountainId).shouldBeTrue()
            state = passAndAdvance(state)

            // 5. COMBAT PHASE - all steps
            state.turnState.step shouldBe Step.BEGIN_COMBAT
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.DECLARE_ATTACKERS
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.DECLARE_BLOCKERS
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.COMBAT_DAMAGE
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            state.turnState.step shouldBe Step.END_COMBAT
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            // 6. POSTCOMBAT MAIN PHASE
            state.turnState.step shouldBe Step.POSTCOMBAT_MAIN
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            // 7. END STEP
            state.turnState.step shouldBe Step.END
            visitedSteps.add(state.turnState.step)
            state = passAndAdvance(state)

            // 8. CLEANUP STEP (no priority - manual advance needed)
            state.turnState.step shouldBe Step.CLEANUP
            visitedSteps.add(state.turnState.step)
            state = advanceFromCleanup(state) // CLEANUP -> turn 2 UNTAP

            // === TURN 2 starts (Player 2) ===

            // UNTAP step (no priority - manual advance needed)
            state.turnState.step shouldBe Step.UNTAP
            visitedSteps.add(state.turnState.step)
            state = advanceFromUntap(state) // UNTAP -> UPKEEP

            // === VERIFICATION ===

            // All 14 steps were visited (including CLEANUP and turn 2 UNTAP)
            visitedSteps.size shouldBe 14
            visitedSteps shouldBe listOf(
                Step.UNTAP,
                Step.UPKEEP,
                Step.DRAW,
                Step.PRECOMBAT_MAIN,
                Step.BEGIN_COMBAT,
                Step.DECLARE_ATTACKERS,
                Step.DECLARE_BLOCKERS,
                Step.FIRST_STRIKE_COMBAT_DAMAGE,
                Step.COMBAT_DAMAGE,
                Step.END_COMBAT,
                Step.POSTCOMBAT_MAIN,
                Step.END,
                Step.CLEANUP,
                Step.UNTAP  // Turn 2 UNTAP
            )

            // Turn ended, now Player 2's turn at UPKEEP
            state.turnState.turnNumber shouldBe 2
            state.turnState.activePlayer shouldBe player2Id
            state.turnState.priorityPlayer shouldBe player2Id
            state.turnState.step shouldBe Step.UPKEEP
            state.turnState.phase shouldBe Phase.BEGINNING

            // Land remains on battlefield
            state.isOnBattlefield(mountainId).shouldBeTrue()
        }

        test("land play is validated - only during main phase with empty stack") {
            var state = newGame()
            val (mountainId, stateWithMountain) = state.putCardInHand(mountainDef, player1Id)
            state = stateWithMountain

            // Advance to UPKEEP
            state = advanceFromUntap(state)
            state.turnState.step shouldBe Step.UPKEEP
            Step.UPKEEP.isMainPhase.shouldBeFalse()

            // Verify main phase check
            state.turnState.step.isMainPhase.shouldBeFalse()

            // Advance to main phase for successful play
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state.turnState.step shouldBe Step.PRECOMBAT_MAIN
            state.turnState.step.isMainPhase.shouldBeTrue()

            val result = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountainId, player1Id))
            result.shouldBeInstanceOf<GameActionResult.Success>()
        }

        test("only one land per turn can be played") {
            var state = newGame()
            // Put two Mountains in hand
            val (mountain1Id, state1) = state.putCardInHand(mountainDef, player1Id)
            val (mountain2Id, state2) = state1.putCardInHand(mountainDef, player1Id)
            state = state2

            // Advance to main phase
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN
            state.turnState.step shouldBe Step.PRECOMBAT_MAIN

            // Play first land - should succeed
            state.canPlayLand(player1Id).shouldBeTrue()
            val result1 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountain1Id, player1Id))
            result1.shouldBeInstanceOf<GameActionResult.Success>()
            state = (result1 as GameActionResult.Success).state

            // Try to play second land - should fail
            state.canPlayLand(player1Id).shouldBeFalse()
            state.getLandsPlayed(player1Id) shouldBe 1

            val result2 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountain2Id, player1Id))
            result2.shouldBeInstanceOf<GameActionResult.Failure>()
            (result2 as GameActionResult.Failure).reason shouldBe "Cannot play another land this turn"
        }

        test("lands played counter resets at start of each turn") {
            var state = newGame()
            val (mountainId, stateWithMountain) = state.putCardInHand(mountainDef, player1Id)
            state = stateWithMountain

            // Advance to main phase and play land
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN

            val playResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountainId, player1Id))
            state = (playResult as GameActionResult.Success).state

            state.getLandsPlayed(player1Id) shouldBe 1

            // Reset lands played (turn-based action at beginning of turn)
            val resetResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResetLandsPlayed(player1Id))
            state = (resetResult as GameActionResult.Success).state

            state.getLandsPlayed(player1Id) shouldBe 0
            state.canPlayLand(player1Id).shouldBeTrue()
        }

        test("priority starts with active player and cycles correctly") {
            var state = newGame()

            // Advance to UPKEEP
            state = advanceFromUntap(state)
            state.turnState.step shouldBe Step.UPKEEP

            // Active player has priority first
            state.turnState.priorityPlayer shouldBe player1Id
            state.turnState.activePlayer shouldBe player1Id

            // Player 1 passes
            val result1 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PassPriority(player1Id))
            state = (result1 as GameActionResult.Success).state

            // Priority moves to player 2
            state.turnState.priorityPlayer shouldBe player2Id
            state.turnState.consecutivePasses shouldBe 1

            // Player 2 passes
            val result2 = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PassPriority(player2Id))
            state = (result2 as GameActionResult.Success).state

            // Both have passed
            state.turnState.consecutivePasses shouldBe 2
            state.turnState.allPlayersPassed().shouldBeTrue()
        }

        test("untap step untaps all permanents controlled by active player") {
            var state = newGame()
            // Put a Mountain in hand
            val (mountainId, stateWithMountain) = state.putCardInHand(mountainDef, player1Id)
            state = stateWithMountain

            // Advance to main phase
            state = advanceFromUntap(state)
            state = passAndAdvance(state) // UPKEEP -> DRAW
            state = passAndAdvance(state) // DRAW -> PRECOMBAT_MAIN

            // Play the land
            val playResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PlayLand(mountainId, player1Id))
            state = (playResult as GameActionResult.Success).state

            // Tap the mountain
            val tapResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, Tap(mountainId))
            state = (tapResult as GameActionResult.Success).state
            state.hasComponent<TappedComponent>(mountainId).shouldBeTrue()

            // Execute UntapAll for the active player
            val untapResult = com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, UntapAll(player1Id))
            state = (untapResult as GameActionResult.Success).state

            // Mountain should be untapped
            state.hasComponent<TappedComponent>(mountainId).shouldBeFalse()
        }
    }

    context("step properties verification") {

        test("untap and cleanup steps have no priority") {
            Step.UNTAP.hasPriority.shouldBeFalse()
            Step.CLEANUP.hasPriority.shouldBeFalse()
        }

        test("all other steps have priority") {
            val stepsWithPriority = listOf(
                Step.UPKEEP,
                Step.DRAW,
                Step.PRECOMBAT_MAIN,
                Step.BEGIN_COMBAT,
                Step.DECLARE_ATTACKERS,
                Step.DECLARE_BLOCKERS,
                Step.FIRST_STRIKE_COMBAT_DAMAGE,
                Step.COMBAT_DAMAGE,
                Step.END_COMBAT,
                Step.POSTCOMBAT_MAIN,
                Step.END
            )

            for (step in stepsWithPriority) {
                step.hasPriority.shouldBeTrue()
            }
        }

        test("only main phases allow sorcery-speed actions") {
            Step.PRECOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()
            Step.POSTCOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()

            // All other steps don't allow sorcery speed
            val nonMainSteps = Step.entries.filter { !it.isMainPhase }
            for (step in nonMainSteps) {
                step.allowsSorcerySpeed.shouldBeFalse()
            }
        }

        test("step.next() returns correct sequence for all 13 steps") {
            Step.UNTAP.next() shouldBe Step.UPKEEP
            Step.UPKEEP.next() shouldBe Step.DRAW
            Step.DRAW.next() shouldBe Step.PRECOMBAT_MAIN
            Step.PRECOMBAT_MAIN.next() shouldBe Step.BEGIN_COMBAT
            Step.BEGIN_COMBAT.next() shouldBe Step.DECLARE_ATTACKERS
            Step.DECLARE_ATTACKERS.next() shouldBe Step.DECLARE_BLOCKERS
            Step.DECLARE_BLOCKERS.next() shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
            Step.FIRST_STRIKE_COMBAT_DAMAGE.next() shouldBe Step.COMBAT_DAMAGE
            Step.COMBAT_DAMAGE.next() shouldBe Step.END_COMBAT
            Step.END_COMBAT.next() shouldBe Step.POSTCOMBAT_MAIN
            Step.POSTCOMBAT_MAIN.next() shouldBe Step.END
            Step.END.next() shouldBe Step.CLEANUP
            Step.CLEANUP.next() shouldBe Step.UNTAP  // Wraps around
        }
    }

    context("CR 500.1 - five phase structure") {

        test("turn contains exactly five phases in correct order") {
            Phase.entries.size shouldBe 5
            Phase.entries shouldBe listOf(
                Phase.BEGINNING,
                Phase.PRECOMBAT_MAIN,
                Phase.COMBAT,
                Phase.POSTCOMBAT_MAIN,
                Phase.ENDING
            )
        }

        test("each step belongs to correct phase") {
            // Beginning phase (3 steps)
            Step.UNTAP.phase shouldBe Phase.BEGINNING
            Step.UPKEEP.phase shouldBe Phase.BEGINNING
            Step.DRAW.phase shouldBe Phase.BEGINNING

            // Precombat main phase (1 step)
            Step.PRECOMBAT_MAIN.phase shouldBe Phase.PRECOMBAT_MAIN

            // Combat phase (6 steps)
            Step.BEGIN_COMBAT.phase shouldBe Phase.COMBAT
            Step.DECLARE_ATTACKERS.phase shouldBe Phase.COMBAT
            Step.DECLARE_BLOCKERS.phase shouldBe Phase.COMBAT
            Step.FIRST_STRIKE_COMBAT_DAMAGE.phase shouldBe Phase.COMBAT
            Step.COMBAT_DAMAGE.phase shouldBe Phase.COMBAT
            Step.END_COMBAT.phase shouldBe Phase.COMBAT

            // Postcombat main phase (1 step)
            Step.POSTCOMBAT_MAIN.phase shouldBe Phase.POSTCOMBAT_MAIN

            // Ending phase (2 steps)
            Step.END.phase shouldBe Phase.ENDING
            Step.CLEANUP.phase shouldBe Phase.ENDING
        }

        test("phase transitions occur correctly through priority passing") {
            var state = newGame()

            // Start at UNTAP (BEGINNING phase)
            state.turnState.phase shouldBe Phase.BEGINNING
            state = advanceFromUntap(state)

            // UPKEEP still in BEGINNING
            state.turnState.step shouldBe Step.UPKEEP
            state.turnState.phase shouldBe Phase.BEGINNING
            state = passAndAdvance(state)

            // DRAW still in BEGINNING
            state.turnState.step shouldBe Step.DRAW
            state.turnState.phase shouldBe Phase.BEGINNING
            state = passAndAdvance(state)

            // PRECOMBAT_MAIN - now in PRECOMBAT_MAIN phase
            state.turnState.step shouldBe Step.PRECOMBAT_MAIN
            state.turnState.phase shouldBe Phase.PRECOMBAT_MAIN
            state = passAndAdvance(state)

            // BEGIN_COMBAT - now in COMBAT phase
            state.turnState.step shouldBe Step.BEGIN_COMBAT
            state.turnState.phase shouldBe Phase.COMBAT
        }
    }
})
