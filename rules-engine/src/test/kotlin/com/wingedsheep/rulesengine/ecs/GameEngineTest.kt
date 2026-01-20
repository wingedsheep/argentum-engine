package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.action.GameActionResult
import com.wingedsheep.rulesengine.ecs.action.PassPriority
import com.wingedsheep.rulesengine.ecs.action.PlayLand
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GameEngineTest : FunSpec({

    // =========================================================================
    // Helpers & Fixtures
    // =========================================================================

    val p1Id = EntityId.of("p1")
    val p2Id = EntityId.of("p2")
    val players = listOf(p1Id to "Alice", p2Id to "Bob")

    // A dummy land card for testing
    val landDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    // A dummy creature card
    val grizzlyDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    fun createStandardGame(): GameState {
        return GameEngine.createGame(players, startingPlayerId = p1Id)
    }

    /**
     * Helper to manually advance turn state to the end of a turn
     * so that the next `resolvePassedPriority` triggers a turn rollover.
     */
    fun GameState.readyProcessingForTurnChange(): GameState {
        // Set phase to CLEANUP (last step)
        return this.copy(
            turnState = this.turnState.copy(
                phase = Phase.ENDING,
                step = Step.CLEANUP,
                // Ensure pass count is high enough so next resolve triggers advance
                consecutivePasses = this.getPlayerIds().size
            )
        )
    }

    /**
     * Helper to inject a card into a player's hand.
     */
    fun GameState.withLandInHand(playerId: EntityId): Pair<GameState, EntityId> {
        val cardId = EntityId.generate()
        val handZone = ZoneId.hand(playerId)

        // Pass components as explicit varargs (no list)
        val (newId, newState) = this.createEntity(
            cardId,
            CardComponent(landDef, playerId),
            ControllerComponent(playerId)
        )

        return newState.addToZone(newId, handZone) to newId
    }

    // =========================================================================
    // Tests
    // =========================================================================

    context("Game Lifecycle") {
        test("createGame initializes players and zones correctly") {
            val state = createStandardGame()

            state.getPlayerIds() shouldHaveSize 2
            state.entities[p1Id]?.get<PlayerComponent>()?.name shouldBe "Alice"
            state.entities[p2Id]?.get<PlayerComponent>()?.name shouldBe "Bob"

            // Verify zones exist
            state.zones[ZoneId.hand(p1Id)] shouldNotBe null
            state.zones[ZoneId.library(p1Id)] shouldNotBe null
            state.zones[ZoneId.graveyard(p1Id)] shouldNotBe null
            state.zones[ZoneId.BATTLEFIELD] shouldNotBe null
            state.zones[ZoneId.STACK] shouldNotBe null
        }

        test("setupGame draws initial hands") {
            val state = createStandardGame()

            // Add some dummy cards to libraries so drawing works
            var primedState = state
            repeat(20) {
                // Pass components as varargs
                val (cId, s) = primedState.createEntity(
                    EntityId.generate(),
                    CardComponent(landDef, p1Id),
                    ControllerComponent(p1Id)
                )
                primedState = s.addToZone(cId, ZoneId.library(p1Id))
            }
            repeat(20) {
                val (cId, s) = primedState.createEntity(
                    EntityId.generate(),
                    CardComponent(landDef, p2Id),
                    ControllerComponent(p2Id)
                )
                primedState = s.addToZone(cId, ZoneId.library(p2Id))
            }

            val result = GameEngine.setupGame(primedState)

            result.shouldBeInstanceOf<SetupResult.Success>()
            val finalState = (result as SetupResult.Success).state

            finalState.getHand(p1Id) shouldHaveSize 7
            finalState.getHand(p2Id) shouldHaveSize 7
        }
    }

    context("Turn Maintenance") {
        test("Lands played count resets at start of new turn") {
            // 1. Setup a game at the end of Turn 1
            var state = createStandardGame()

            // Mark P1 as having played a land
            state = state.updateEntity(p1Id) {
                it.with(LandsPlayedComponent(count = 1))
            }

            // Verify P1 cannot play land
            state.canPlayLand(p1Id) shouldBe false

            // 2. Prepare transition to Turn 2
            state = state.readyProcessingForTurnChange()

            // 3. Trigger resolution (should roll over to Turn 2)
            val nextTurnState = GameEngine.resolvePassedPriority(state)

            nextTurnState.turnNumber shouldBe 2

            // 4. Verify LandsPlayedComponent was reset for P1
            val p1Lands = nextTurnState.getComponent<LandsPlayedComponent>(p1Id)
            p1Lands.shouldNotBeNull()
            p1Lands.count shouldBe 0
            p1Lands.canPlayLand shouldBe true
        }

        test("Summoning sickness is removed from active player's creatures at start of turn") {
            var state = createStandardGame()

            // P2 puts a creature onto battlefield with sickness
            val creatureId = EntityId.generate()

            val (cId, s) = state.createEntity(
                creatureId,
                CardComponent(grizzlyDef, p2Id),
                ControllerComponent(p2Id),
                SummoningSicknessComponent
            )
            state = s.addToBattlefield(cId)

            // Verify sickness exists
            state.hasComponent<SummoningSicknessComponent>(creatureId) shouldBe true

            // Advance to Turn 2 (P2 becomes active)
            state = state.readyProcessingForTurnChange()
            val nextTurnState = GameEngine.resolvePassedPriority(state)

            nextTurnState.activePlayerId shouldBe p2Id

            // Verify sickness is gone since it became P2's turn
            nextTurnState.hasComponent<SummoningSicknessComponent>(creatureId) shouldBe false
        }
    }

    context("Mulligan") {
        test("startMulligan shuffles hand back and draws 7") {
            var state = createStandardGame()
            // Add 7 cards to hand, 10 to library
            val handIds = (1..7).map { EntityId.generate() }
            val libIds = (1..10).map { EntityId.generate() }

            // Populate state
            handIds.forEach { id ->
                val (newId, newState) = state.createEntity(id, CardComponent(landDef, p1Id))
                state = newState.addToZone(newId, ZoneId.hand(p1Id))
            }
            libIds.forEach { id ->
                val (newId, newState) = state.createEntity(id, CardComponent(landDef, p1Id))
                state = newState.addToZone(newId, ZoneId.library(p1Id))
            }

            val result = GameEngine.startMulligan(state, p1Id, 1)

            result.shouldBeInstanceOf<MulliganResult.Success>()
            val success = result as MulliganResult.Success

            success.state.getHand(p1Id) shouldHaveSize 7
            success.cardsToPutOnBottom shouldBe 1
            success.state.getLibrarySize(p1Id) shouldBe 10
        }

        test("executeMulligan moves cards to bottom of library") {
            var state = createStandardGame()
            val c1 = EntityId.generate()
            val c2 = EntityId.generate() // card to bottom

            // Chain state creation with components
            state = state.createEntity(c1, CardComponent(landDef, p1Id)).second
            state = state.createEntity(c2, CardComponent(landDef, p1Id)).second
            state = state.addToHand(p1Id, c1)
            state = state.addToHand(p1Id, c2)

            state.getHand(p1Id) shouldHaveSize 2
            state.getLibrary(p1Id).shouldBeEmpty()

            val result = GameEngine.executeMulligan(state, p1Id, listOf(c2))

            result.shouldBeInstanceOf<MulliganResult.Success>()
            val success = result as MulliganResult.Success
            val newState = success.state

            newState.getHand(p1Id) shouldHaveSize 1
            newState.getHand(p1Id) shouldContain c1
            newState.getLibrary(p1Id) shouldHaveSize 1
            newState.getLibrary(p1Id) shouldContain c2
            // Verify c2 is at bottom (index 0)
            newState.getBottomOfZone(ZoneId.library(p1Id)) shouldBe c2
        }
    }

    context("Priority and Stacks") {
        test("Passing priority moves to next player") {
            val state = createStandardGame()

            val result = GameEngine.executePlayerAction(state, PassPriority(p1Id))

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            val newState = success.state

            newState.priorityPlayerId shouldBe p2Id
            newState.turnState.consecutivePasses shouldBe 1
        }

        test("All players passing resolves stack item if stack not empty") {
            var state = createStandardGame()

            // 1. Put something on the stack (dummy spell)
            val spellId = EntityId.generate()

            val (sId, s) = state.createEntity(spellId, CardComponent(landDef, p1Id))
            state = s

            // Add SpellOnStack component manually
            state = state.addComponent(spellId, SpellOnStackComponent(casterId = p1Id))
            state = state.addToStack(spellId)

            // 2. Set state so all players passed
            state = state.copy(
                turnState = state.turnState.copy(consecutivePasses = 2)
            )

            // 3. Resolve
            val newState = GameEngine.resolvePassedPriority(state)

            // 4. Assert - should be removed from stack (likely moved to graveyard as resolved spell)
            newState.getStack().isEmpty() shouldBe true
            newState.turnState.consecutivePasses shouldBe 0
        }

        test("All players passing advances phase if stack empty") {
            var state = createStandardGame()
            // Force state to beginning
            state = state.copy(
                turnState = state.turnState.copy(phase = Phase.BEGINNING, step = Step.UNTAP, consecutivePasses = 2)
            )

            val newState = GameEngine.resolvePassedPriority(state)

            newState.currentStep shouldBe Step.UPKEEP
            newState.turnState.consecutivePasses shouldBe 0
        }
    }

    context("Integration: Land Play Logic") {
        test("Playing a land updates state correctly") {
            var state = createStandardGame()

            // Setup hand
            val (s, landId) = state.withLandInHand(p1Id)
            // Advance to Main Phase so land play is legal
            state = s.copy(
                turnState = s.turnState.copy(step = Step.PRECOMBAT_MAIN)
            )

            // Action: P1 plays land
            val action = PlayLand(landId, p1Id)
            val result = GameEngine.executePlayerAction(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getBattlefield() shouldContain landId
            success.state.getHand(p1Id).shouldBeEmpty()

            val landsPlayed = success.state.getComponent<LandsPlayedComponent>(p1Id)
            landsPlayed?.count shouldBe 1
        }

        test("Cannot play land if limit reached") {
            var state = createStandardGame()
            val (s, landId) = state.withLandInHand(p1Id)

            // Set limit reached
            state = s.copy(turnState = s.turnState.copy(step = Step.PRECOMBAT_MAIN))
            state = state.updateEntity(p1Id) { it.with(LandsPlayedComponent(count = 1)) }

            val action = PlayLand(landId, p1Id)
            val result = GameEngine.executePlayerAction(state, action)

            result.shouldBeInstanceOf<GameActionResult.Failure>()
        }
    }
})
