package com.wingedsheep.rulesengine.scenarios

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.*
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.stack.StackResolver
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario 2.2: The LIFO Verification ("Counterspell War")
 * Target System: Stack Object Management
 * Complexity: High
 *
 * This scenario validates the core data structure of the stack. The engine must
 * treat the stack not as a queue, but as a dynamic array where items are added
 * to the top and resolved from the top, with priority rounds in between each resolution.
 *
 * Setup:
 * - AP: Player A. NAP: Player B.
 * - Hand A: Wrath of God (Sorcery), Counterspell (Instant).
 * - Hand B: Counterspell (Instant).
 * - Mana: Infinite for both.
 * - Battlefield: Creatures for both players (to verify Wrath of God effect)
 *
 * Action Sequence:
 * 1. Player A casts Wrath of God. Stack: [Wrath]
 * 2. Player B casts Counterspell targeting Wrath of God. Stack: [Wrath, B's Counter]
 * 3. Player A casts Counterspell targeting Player B's Counterspell. Stack: [Wrath, B's Counter, A's Counter]
 * 4. Resolution: Both players pass. Top item (A's Counterspell) resolves.
 *
 * Expected Outcomes:
 * - Resolution 1: Player A's Counterspell resolves, countering Player B's Counterspell.
 * - State Update: Player B's Counterspell moves to graveyard (does NOT resolve).
 * - Priority Check: After resolution, priority returns to active player (A).
 * - Resolution 2: Both players pass. Wrath of God resolves.
 * - Final State: All creatures are destroyed.
 *
 * Rules Analysis: Tests LIFO architecture. When B's Counterspell is countered,
 * it is removed from the stack without resolving. The engine must properly manage
 * priority after each resolution.
 */
class CounterspellWarTest : FunSpec({

    val player1Id = EntityId.of("player1")  // Player A (Active Player)
    val player2Id = EntityId.of("player2")  // Player B (Non-Active Player)

    // Wrath of God - {2}{W}{W} Sorcery - "Destroy all creatures."
    val wrathOfGodDef = CardDefinition.sorcery(
        name = "Wrath of God",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        oracleText = "Destroy all creatures. They can't be regenerated."
    )

    // Counterspell - {U}{U} Instant - "Counter target spell."
    val counterspellDef = CardDefinition.instant(
        name = "Counterspell",
        manaCost = ManaCost.parse("{U}{U}"),
        oracleText = "Counter target spell."
    )

    // Test creature - simple 2/2
    val bearsDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    // Card scripts
    val wrathScript = cardScript("Wrath of God") {
        spell(DestroyAllCreaturesEffect)
    }

    val counterspellScript = cardScript("Counterspell") {
        spell(CounterSpellEffect)
    }

    // Script repository containing both spells
    val scriptRepository = CardScriptRepository().apply {
        register(wrathScript)
        register(counterspellScript)
    }

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    /**
     * Helper to have both players pass priority and let the engine advance.
     */
    fun passAndAdvance(state: GameState): GameState {
        val result1 = GameEngine.executeAction(state, PassPriority(state.turnState.priorityPlayer))
        var currentState = (result1 as GameActionResult.Success).state

        val result2 = GameEngine.executeAction(currentState, PassPriority(currentState.turnState.priorityPlayer))
        currentState = (result2 as GameActionResult.Success).state

        currentState.turnState.allPlayersPassed().shouldBeTrue()
        return GameEngine.resolvePassedPriority(currentState)
    }

    /**
     * Helper to add a spell card to a player's hand.
     */
    fun GameState.addSpellToHand(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = listOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        return cardId to state1.addToZone(cardId, ZoneId.hand(ownerId))
    }

    /**
     * Helper to add a creature to the battlefield.
     */
    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = listOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (creatureId, state1) = createEntity(EntityId.generate(), components)
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    /**
     * Helper to give a player "infinite" mana (enough for any spell).
     */
    fun GameState.giveInfiniteMana(playerId: EntityId): GameState {
        return updateEntity(playerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(
                manaPool
                    .add(Color.WHITE, 10)
                    .add(Color.BLUE, 10)
                    .add(Color.BLACK, 10)
                    .add(Color.RED, 10)
                    .add(Color.GREEN, 10)
            )
        }
    }

    /**
     * Creates a game state in the precombat main phase ready for sorcery-speed casting.
     */
    fun createGameInMainPhase(): GameState {
        var state = newGame()

        // UNTAP step has no priority
        state.turnState.step shouldBe Step.UNTAP
        state = state.copy(turnState = state.turnState.advanceStep())

        // UPKEEP - both pass
        state.turnState.step shouldBe Step.UPKEEP
        state = passAndAdvance(state)

        // DRAW - both pass
        state.turnState.step shouldBe Step.DRAW
        state = passAndAdvance(state)

        // PRECOMBAT_MAIN
        state.turnState.step shouldBe Step.PRECOMBAT_MAIN
        return state
    }

    context("Scenario 2.2: The LIFO Verification (Counterspell War)") {

        test("setup: both players have spells and creatures") {
            var state = createGameInMainPhase()

            // Add Wrath of God and Counterspell to Player A's hand
            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            state = state2

            // Add Counterspell to Player B's hand
            val (counter2Id, state3) = state.addSpellToHand(counterspellDef, player2Id)
            state = state3

            // Add creatures to both players
            val (bears1Id, state4) = state.addCreatureToBattlefield(bearsDef, player1Id)
            val (bears2Id, state5) = state4.addCreatureToBattlefield(bearsDef, player2Id)
            state = state5

            // Give both players infinite mana
            state = state.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Verify setup
            state.getHand(player1Id).contains(wrathId).shouldBeTrue()
            state.getHand(player1Id).contains(counter1Id).shouldBeTrue()
            state.getHand(player2Id).contains(counter2Id).shouldBeTrue()
            state.isOnBattlefield(bears1Id).shouldBeTrue()
            state.isOnBattlefield(bears2Id).shouldBeTrue()
        }

        test("stack builds up in correct order: Wrath -> B's Counter -> A's Counter") {
            var state = createGameInMainPhase()

            // Setup
            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            state = state3.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Step 1: Player A casts Wrath of God
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = wrathId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id)
                )
            ) as GameActionResult.Success).state

            // Stack: [Wrath]
            state.getStack() shouldHaveSize 1
            state.getStack().first() shouldBe wrathId

            // Step 2: Player B casts Counterspell targeting Wrath
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = counter2Id,
                    casterId = player2Id,
                    fromZone = ZoneId.hand(player2Id),
                    targets = listOf(ChosenTarget.Spell(wrathId))
                )
            ) as GameActionResult.Success).state

            // Stack: [Wrath, B's Counter] (B's Counter on top)
            state.getStack() shouldHaveSize 2
            state.getTopOfStack() shouldBe counter2Id

            // Step 3: Player A casts Counterspell targeting B's Counterspell
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = counter1Id,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Spell(counter2Id))
                )
            ) as GameActionResult.Success).state

            // Stack: [Wrath, B's Counter, A's Counter] (A's Counter on top)
            state.getStack() shouldHaveSize 3
            state.getTopOfStack() shouldBe counter1Id

            // Verify stack order (bottom to top)
            val stack = state.getStack()
            stack[0] shouldBe wrathId      // Bottom
            stack[1] shouldBe counter2Id   // Middle
            stack[2] shouldBe counter1Id   // Top
        }

        test("LIFO resolution: A's Counter resolves first, countering B's Counter") {
            var state = createGameInMainPhase()

            // Setup
            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            state = state3.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Build the stack: Wrath -> B's Counter -> A's Counter
            state = (GameEngine.executeAction(state, CastSpell(wrathId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter2Id, player2Id, ZoneId.hand(player2Id), listOf(ChosenTarget.Spell(wrathId)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(counter2Id)))) as GameActionResult.Success).state

            state.getStack() shouldHaveSize 3

            // === Resolution 1: A's Counterspell resolves ===
            val resolver = StackResolver(scriptRepository = scriptRepository)
            val result1 = resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved
            state = result1.state

            // B's Counterspell should be countered (removed from stack, moved to graveyard)
            state.getStack() shouldHaveSize 1  // Only Wrath remains
            state.getStack().contains(counter2Id).shouldBeFalse()  // B's Counter NOT on stack
            state.getGraveyard(player2Id) shouldContain counter2Id  // B's Counter in B's graveyard
            state.getGraveyard(player1Id) shouldContain counter1Id  // A's Counter in A's graveyard (resolved)

            // Wrath of God is still on the stack
            state.getStack().first() shouldBe wrathId
        }

        test("countered spell does NOT resolve (no effects happen)") {
            var state = createGameInMainPhase()

            // Setup with creatures
            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            val (bears1Id, state4) = state3.addCreatureToBattlefield(bearsDef, player1Id)
            val (bears2Id, state5) = state4.addCreatureToBattlefield(bearsDef, player2Id)
            state = state5.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Build stack and resolve A's Counterspell (counters B's Counter)
            state = (GameEngine.executeAction(state, CastSpell(wrathId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter2Id, player2Id, ZoneId.hand(player2Id), listOf(ChosenTarget.Spell(wrathId)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(counter2Id)))) as GameActionResult.Success).state

            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // At this point, B's Counterspell was countered.
            // If B's Counterspell had resolved, Wrath would have been countered.
            // But since it was countered, Wrath is still on the stack.

            // Wrath is still on stack (not countered)
            state.getStack() shouldContain wrathId
            state.getStack() shouldHaveSize 1

            // Now resolve Wrath of God
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // SBA check
            state = (GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            // All creatures should be destroyed
            state.isOnBattlefield(bears1Id).shouldBeFalse()
            state.isOnBattlefield(bears2Id).shouldBeFalse()
            state.getGraveyard(player1Id) shouldContain bears1Id
            state.getGraveyard(player2Id) shouldContain bears2Id
        }

        test("complete counterspell war with Wrath resolving") {
            var state = createGameInMainPhase()

            // Setup
            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            val (bears1Id, state4) = state3.addCreatureToBattlefield(bearsDef, player1Id)
            val (bears2Id, state5) = state4.addCreatureToBattlefield(bearsDef, player2Id)
            state = state5.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // === Step 1: Player A casts Wrath of God ===
            state = (GameEngine.executeAction(state, CastSpell(wrathId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state

            // === Step 2: Player B responds with Counterspell ===
            state = (GameEngine.executeAction(state, CastSpell(counter2Id, player2Id, ZoneId.hand(player2Id), listOf(ChosenTarget.Spell(wrathId)))) as GameActionResult.Success).state

            // === Step 3: Player A responds with Counterspell ===
            state = (GameEngine.executeAction(state, CastSpell(counter1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(counter2Id)))) as GameActionResult.Success).state

            // Stack: [Wrath, B's Counter, A's Counter]
            state.getStack() shouldHaveSize 3

            // === Resolution Phase ===
            val resolver = StackResolver(scriptRepository = scriptRepository)

            // Resolution 1: A's Counterspell resolves
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state
            state.getStack() shouldHaveSize 1  // Only Wrath
            state.getGraveyard(player2Id) shouldContain counter2Id  // B's Counter countered
            state.getGraveyard(player1Id) shouldContain counter1Id  // A's Counter resolved

            // Resolution 2: Wrath of God resolves
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state
            state.getStack().shouldBeEmpty()
            state.getGraveyard(player1Id) shouldContain wrathId

            // === Final State Check ===
            // All creatures destroyed
            state = (GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state
            state.isOnBattlefield(bears1Id).shouldBeFalse()
            state.isOnBattlefield(bears2Id).shouldBeFalse()

            // Final graveyard contents
            state.getGraveyard(player1Id) shouldContain wrathId
            state.getGraveyard(player1Id) shouldContain counter1Id
            state.getGraveyard(player1Id) shouldContain bears1Id
            state.getGraveyard(player2Id) shouldContain counter2Id
            state.getGraveyard(player2Id) shouldContain bears2Id
        }
    }

    context("LIFO Architecture Verification") {

        test("stack is truly LIFO - last in, first out") {
            var state = createGameInMainPhase()

            // Add multiple spells to hand
            val (spell1Id, state1) = state.addSpellToHand(counterspellDef, player1Id)
            val (spell2Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (spell3Id, state3) = state2.addSpellToHand(counterspellDef, player1Id)
            state = state3.giveInfiniteMana(player1Id)

            // Create a dummy target spell
            val (targetId, state4) = state.addSpellToHand(wrathOfGodDef, player1Id)
            state = (GameEngine.executeAction(state4, CastSpell(targetId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state

            // Cast spells in order 1, 2, 3
            state = (GameEngine.executeAction(state, CastSpell(spell1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(targetId)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(spell2Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(spell1Id)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(spell3Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(spell2Id)))) as GameActionResult.Success).state

            // Stack order should be [target, spell1, spell2, spell3] (bottom to top)
            val stack = state.getStack()
            stack shouldHaveSize 4
            stack[0] shouldBe targetId  // First cast - bottom
            stack[1] shouldBe spell1Id
            stack[2] shouldBe spell2Id
            stack[3] shouldBe spell3Id  // Last cast - top

            // Top of stack should be spell3 (LIFO)
            state.getTopOfStack() shouldBe spell3Id
        }

        test("countered spell is removed from stack mid-resolution") {
            var state = createGameInMainPhase()

            val (wrathId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            state = state3.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Build stack: Wrath -> B's Counter -> A's Counter
            state = (GameEngine.executeAction(state, CastSpell(wrathId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter2Id, player2Id, ZoneId.hand(player2Id), listOf(ChosenTarget.Spell(wrathId)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(counter2Id)))) as GameActionResult.Success).state

            // Before resolution: 3 items on stack
            state.getStack() shouldHaveSize 3

            // Resolve A's Counterspell (counters B's Counterspell)
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // After resolution:
            // - A's Counter resolved and moved to graveyard
            // - B's Counter was countered and moved to graveyard (removed from stack)
            // - Only Wrath remains on stack
            state.getStack() shouldHaveSize 1
            state.getStack() shouldContain wrathId
            state.getStack().contains(counter2Id).shouldBeFalse()
        }

        test("SpellCountered event is generated when spell is countered") {
            var state = createGameInMainPhase()

            val (targetId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counterId, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            state = state2.giveInfiniteMana(player1Id)

            // Cast target spell
            state = (GameEngine.executeAction(state, CastSpell(targetId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state

            // Cast Counterspell targeting the target spell
            state = (GameEngine.executeAction(state, CastSpell(counterId, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(targetId)))) as GameActionResult.Success).state

            // Resolve Counterspell
            val resolver = StackResolver(scriptRepository = scriptRepository)
            val result = resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved

            // Check for SpellCountered event (wrapped in EffectEventWrapper)
            val effectEvents = result.events.filterIsInstance<StackResolver.StackResolutionEvent.EffectEventWrapper>()
            val counteredEvents = effectEvents.filter {
                it.event is com.wingedsheep.rulesengine.ecs.script.EffectEvent.SpellCountered
            }
            counteredEvents shouldHaveSize 1

            val counteredEvent = counteredEvents.first().event as com.wingedsheep.rulesengine.ecs.script.EffectEvent.SpellCountered
            counteredEvent.spellEntityId shouldBe targetId
            counteredEvent.spellName shouldBe "Wrath of God"
            counteredEvent.ownerId shouldBe player1Id
        }
    }

    context("edge cases") {

        test("counterspell fizzles if target spell is already gone") {
            var state = createGameInMainPhase()

            val (targetId, state1) = state.addSpellToHand(wrathOfGodDef, player1Id)
            val (counter1Id, state2) = state1.addSpellToHand(counterspellDef, player1Id)
            val (counter2Id, state3) = state2.addSpellToHand(counterspellDef, player2Id)
            state = state3.giveInfiniteMana(player1Id).giveInfiniteMana(player2Id)

            // Cast target spell
            state = (GameEngine.executeAction(state, CastSpell(targetId, player1Id, ZoneId.hand(player1Id))) as GameActionResult.Success).state

            // Both players target the same spell with Counterspell
            state = (GameEngine.executeAction(state, CastSpell(counter1Id, player1Id, ZoneId.hand(player1Id), listOf(ChosenTarget.Spell(targetId)))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(counter2Id, player2Id, ZoneId.hand(player2Id), listOf(ChosenTarget.Spell(targetId)))) as GameActionResult.Success).state

            // Stack: [target, counter1, counter2]
            state.getStack() shouldHaveSize 3

            val resolver = StackResolver(scriptRepository = scriptRepository)

            // Resolution 1: counter2 resolves, counters target
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state
            state.getStack() shouldHaveSize 1  // Only counter1 left
            state.getGraveyard(player1Id) shouldContain targetId

            // Resolution 2: counter1 should fizzle (target is gone)
            val result = resolver.resolveTopOfStack(state)
            result.shouldBeInstanceOf<StackResolver.ResolutionResult.Fizzled>()
            state = (result as StackResolver.ResolutionResult.Fizzled).state

            // counter1 goes to graveyard (fizzled)
            state.getStack().shouldBeEmpty()
            state.getGraveyard(player1Id) shouldContain counter1Id
        }
    }
})
