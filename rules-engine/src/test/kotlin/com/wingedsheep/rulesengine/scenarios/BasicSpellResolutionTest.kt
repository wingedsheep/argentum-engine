package com.wingedsheep.rulesengine.scenarios

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
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
 * Scenario 2.1: Basic Spell Resolution (The Uncontested Spell)
 * Target System: Stack Resolution
 * Complexity: Basic
 *
 * A simple targeted instant spell that resolves without being countered.
 *
 * Setup:
 * - Player A has Shock ({R}, Instant, "Deal 2 damage to any target") in hand
 * - Player A has {R} available in mana pool
 * - Player B starts at 20 life
 *
 * Action Sequence:
 * 1. Player A casts Shock targeting Player B
 * 2. Shock goes on the stack
 * 3. Both players pass priority (no responses)
 * 4. Shock resolves
 *
 * Expected Outcomes:
 * - Stack Lifecycle: Shock on stack → resolves → Shock in graveyard
 * - Effect Application: Player B takes 2 damage (now at 18 life)
 * - State: Stack is empty, Shock in Player A's graveyard
 *
 * Rules Analysis: Verifies CR 608 (Resolving Spells and Abilities).
 * Tests basic targeting, stack resolution, and effect application.
 */
class BasicSpellResolutionTest : FunSpec({

    val player1Id = EntityId.of("player1")  // Player A (Casting Shock)
    val player2Id = EntityId.of("player2")  // Player B (Target)

    // Shock - {R} Instant - "Deal 2 damage to any target."
    val shockDef = CardDefinition.instant(
        name = "Shock",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Shock deals 2 damage to any target."
    )

    // Card script for Shock that defines its effect
    val shockScript = cardScript("Shock") {
        spell(DealDamageEffect(2, EffectTarget.AnyTarget))
    }

    // Script repository containing Shock's effect
    val scriptRepository = CardScriptRepository().apply {
        register(shockScript)
    }

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    /**
     * Helper to have both players pass priority and let the engine advance.
     * This is the proper way to test the engine - priority passing drives state transitions.
     */
    fun passAndAdvance(state: GameState): GameState {
        // Active player passes priority
        val result1 = GameEngine.executeAction(state, PassPriority(state.turnState.priorityPlayer))
        var currentState = (result1 as GameActionResult.Success).state

        // Non-active player passes priority
        val result2 = GameEngine.executeAction(currentState, PassPriority(currentState.turnState.priorityPlayer))
        currentState = (result2 as GameActionResult.Success).state

        // All players have passed, engine resolves (advances step or resolves stack)
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

        val (cardId, state1) = createEntity(
            EntityId.generate(),
            components
        )
        return cardId to state1.addToZone(cardId, ZoneId.hand(ownerId))
    }

    /**
     * Creates a game state in the precombat main phase where spells can be cast.
     * Uses proper priority passing to advance through the turn.
     */
    fun createGameInMainPhase(): GameState {
        var state = newGame()

        // UNTAP step has no priority, advance directly
        state.turnState.step shouldBe Step.UNTAP
        state = state.copy(turnState = state.turnState.advanceStep())

        // UPKEEP - both players pass
        state.turnState.step shouldBe Step.UPKEEP
        state = passAndAdvance(state)

        // DRAW - both players pass
        state.turnState.step shouldBe Step.DRAW
        state = passAndAdvance(state)

        // PRECOMBAT_MAIN
        state.turnState.step shouldBe Step.PRECOMBAT_MAIN
        return state
    }

    context("Scenario 2.1: Basic Spell Resolution (The Uncontested Spell)") {

        test("setup: Player A has Shock in hand and red mana available") {
            var state = createGameInMainPhase()

            // Add Shock to Player A's hand
            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            // Add red mana to Player A's mana pool
            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Verify Shock is in hand
            state.getHand(player1Id) shouldContain shockId

            // Verify mana pool has red mana
            val manaPool = state.getComponent<ManaPoolComponent>(player1Id)
            manaPool.shouldNotBeNull()
            manaPool.pool.red shouldBe 1

            // Verify Player B starts at 20 life
            val player2Life = state.getComponent<LifeComponent>(player2Id)
            player2Life.shouldNotBeNull()
            player2Life.life shouldBe 20
        }

        test("casting Shock puts it on the stack") {
            var state = createGameInMainPhase()

            // Add Shock to Player A's hand
            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            // Add red mana to Player A's mana pool
            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock targeting Player B
            val castResult = GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            )

            castResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (castResult as GameActionResult.Success).state

            // Verify Shock is on the stack
            state.getStack() shouldContain shockId
            state.getStack() shouldHaveSize 1

            // Verify Shock is no longer in hand
            state.getHand(player1Id).contains(shockId).shouldBeFalse()

            // Verify Shock has SpellOnStackComponent
            val spellComponent = state.getComponent<SpellOnStackComponent>(shockId)
            spellComponent.shouldNotBeNull()
            spellComponent.casterId shouldBe player1Id
            spellComponent.targets shouldHaveSize 1
            spellComponent.targets.first().shouldBeInstanceOf<ChosenTarget.Player>()
            (spellComponent.targets.first() as ChosenTarget.Player).playerId shouldBe player2Id
        }

        test("complete spell resolution: cast, pass priority, resolve") {
            var state = createGameInMainPhase()

            // === SETUP ===
            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Verify starting life
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 20

            // === STEP 1: CAST SHOCK ===
            val castResult = GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            )
            state = (castResult as GameActionResult.Success).state

            // Shock is on the stack
            state.getStack() shouldContain shockId

            // === STEP 2: RESOLVE VIA STACK RESOLVER ===
            // Both players pass priority - use the StackResolver directly to resolve the spell
            val resolver = StackResolver(scriptRepository = scriptRepository)
            val resolveResult = resolver.resolveTopOfStack(state)

            resolveResult.shouldBeInstanceOf<StackResolver.ResolutionResult.Resolved>()
            state = (resolveResult as StackResolver.ResolutionResult.Resolved).state

            // === VERIFY OUTCOMES ===

            // 1. Stack is now empty
            state.getStack().shouldBeEmpty()

            // 2. Shock is in Player A's graveyard
            state.getGraveyard(player1Id) shouldContain shockId

            // 3. Player B took 2 damage (life is now 18)
            val player2Life = state.getComponent<LifeComponent>(player2Id)
            player2Life.shouldNotBeNull()
            player2Life.life shouldBe 18

            // 4. Shock no longer has SpellOnStackComponent (removed during resolution)
            state.getComponent<SpellOnStackComponent>(shockId).shouldBeNull()
        }

        test("resolution generates correct events") {
            var state = createGameInMainPhase()

            // Setup
            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock
            val castResult = GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            )
            state = (castResult as GameActionResult.Success).state

            // Resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            val resolveResult = resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved

            // Check events
            val spellResolvedEvents = resolveResult.events.filterIsInstance<StackResolver.StackResolutionEvent.SpellResolved>()
            spellResolvedEvents shouldHaveSize 1
            spellResolvedEvents.first().entityId shouldBe shockId
            spellResolvedEvents.first().name shouldBe "Shock"

            val graveyardEvents = resolveResult.events.filterIsInstance<StackResolver.StackResolutionEvent.SpellMovedToGraveyard>()
            graveyardEvents shouldHaveSize 1
            graveyardEvents.first().entityId shouldBe shockId
            graveyardEvents.first().ownerId shouldBe player1Id
        }

        test("multiple spells resolve in LIFO order") {
            var state = createGameInMainPhase()

            // Create two Shock cards
            val (shock1Id, state1) = state.addSpellToHand(shockDef, player1Id)
            val (shock2Id, state2) = state1.addSpellToHand(shockDef, player1Id)
            state = state2

            // Add enough mana for both
            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 2))
            }

            // Cast first Shock targeting Player B
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shock1Id,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Cast second Shock targeting Player B
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shock2Id,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Stack should have both spells (shock2 on top, shock1 below)
            state.getStack() shouldHaveSize 2
            state.getTopOfStack() shouldBe shock2Id

            // Resolve first (shock2 - last in, first out)
            val resolver = StackResolver(scriptRepository = scriptRepository)
            var resolveResult = resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved
            state = resolveResult.state

            // Verify shock2 resolved
            state.getStack() shouldHaveSize 1
            state.getTopOfStack() shouldBe shock1Id
            state.getGraveyard(player1Id) shouldContain shock2Id
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 18

            // Resolve second (shock1)
            resolveResult = resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved
            state = resolveResult.state

            // Verify shock1 resolved
            state.getStack().shouldBeEmpty()
            state.getGraveyard(player1Id) shouldContain shock1Id
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 16
        }
    }

    context("CR 608 - Resolving Spells and Abilities") {

        test("spell effect is applied when it resolves") {
            var state = createGameInMainPhase()

            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Before resolution, Player B still has 20 life
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 20

            // Resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // After resolution, Player B has 18 life
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 18
        }

        test("spell moves to graveyard after resolution") {
            var state = createGameInMainPhase()

            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Shock is on the stack
            state.getStack() shouldContain shockId
            state.getGraveyard(player1Id).contains(shockId).shouldBeFalse()

            // Resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // Shock is now in graveyard, not on stack
            state.getStack().contains(shockId).shouldBeFalse()
            state.getGraveyard(player1Id) shouldContain shockId
        }

        test("spell controller is tracked correctly") {
            var state = createGameInMainPhase()

            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Verify the spell knows who cast it
            val spellComponent = state.getComponent<SpellOnStackComponent>(shockId)
            spellComponent.shouldNotBeNull()
            spellComponent.casterId shouldBe player1Id
        }
    }

    context("edge cases") {

        test("empty stack returns EmptyStack result") {
            var state = createGameInMainPhase()

            // Stack should be empty
            state.getStack().shouldBeEmpty()

            // Try to resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            val result = resolver.resolveTopOfStack(state)

            result.shouldBeInstanceOf<StackResolver.ResolutionResult.EmptyStack>()
        }

        test("spell can target a creature") {
            var state = createGameInMainPhase()

            // Create a creature for Player B
            val creatureDef = CardDefinition.creature(
                name = "Grizzly Bears",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = emptySet(),
                power = 2,
                toughness = 2
            )

            val creatureComponents = listOf<Component>(
                CardComponent(creatureDef, player2Id),
                ControllerComponent(player2Id)
            )
            val (creatureId, state1) = state.createEntity(EntityId.generate(), creatureComponents)
            state = state1.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Add Shock to Player A's hand
            val (shockId, state2) = state.addSpellToHand(shockDef, player1Id)
            state = state2

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock targeting the creature
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Permanent(creatureId))
                )
            ) as GameActionResult.Success).state

            // Resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // Creature took 2 damage
            val creatureDamage = state.getComponent<DamageComponent>(creatureId)
            creatureDamage.shouldNotBeNull()
            creatureDamage.amount shouldBe 2

            // Check SBA - creature should die (2/2 with 2 damage)
            state = (GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            state.isOnBattlefield(creatureId).shouldBeFalse()
            state.getGraveyard(player2Id) shouldContain creatureId
        }

        test("lethal damage to player triggers life check") {
            var state = createGameInMainPhase()

            // Set Player B to 2 life
            state = state.updateEntity(player2Id) { container ->
                container.with(LifeComponent(2))
            }

            val (shockId, state1) = state.addSpellToHand(shockDef, player1Id)
            state = state1

            state = state.updateEntity(player1Id) { container ->
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                container.with(manaPool.add(Color.RED, 1))
            }

            // Cast Shock targeting Player B
            state = (GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = shockId,
                    casterId = player1Id,
                    fromZone = ZoneId.hand(player1Id),
                    targets = listOf(ChosenTarget.Player(player2Id))
                )
            ) as GameActionResult.Success).state

            // Resolve
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // Player B now has 0 life
            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 0

            // Check SBA - Player B should lose the game
            val sbaResult = GameEngine.executeAction(state, CheckStateBasedActions())
            sbaResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (sbaResult as GameActionResult.Success).state

            // Player B should have lost
            state.hasComponent<LostGameComponent>(player2Id).shouldBeTrue()
        }
    }
})
