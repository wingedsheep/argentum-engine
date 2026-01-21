package com.wingedsheep.rulesengine.scenarios

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.*
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.stack.StackResolver
import com.wingedsheep.rulesengine.ecs.combat.CombatValidator
import com.wingedsheep.rulesengine.ability.CardScriptRepository
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
 * Scenario 3.2: The Flash Ambush (Declare Attackers Step)
 * Target System: Step Transition / Flash Interaction
 * Complexity: Intermediate
 *
 * This scenario verifies the existence of the priority window inside the Declare
 * Attackers step, allowing the Defending Player to cast creatures with Flash to block.
 *
 * Setup:
 * - AP: Player A. NAP: Player B.
 * - Battlefield A: Grizzly Bears (2/2).
 * - Hand B: Restoration Angel (3/4 Flying Flash creature).
 * - Current State: Beginning of Combat Step.
 *
 * Action Sequence:
 * 1. Transition: Players pass in Beginning of Combat. Engine enters Declare Attackers Step.
 * 2. Turn-Based Action: Player A declares Grizzly Bears as attacking. (Engine taps Grizzly Bears).
 * 3. Priority: Player A passes priority.
 * 4. Input: Player B casts Restoration Angel.
 * 5. Resolution: Players pass. Restoration Angel resolves and enters the battlefield.
 * 6. Transition: Players pass. Engine enters Declare Blockers Step.
 * 7. Turn-Based Action: Player B declares Restoration Angel blocking Grizzly Bears.
 *
 * Expected Outcomes:
 * - Legality: The engine must recognize Restoration Angel as a valid blocker.
 * - Timing: The engine must not auto-skip from Attackers to Blockers. It must verify
 *   that the NAP has a chance to act.
 * - Constraint: If Player B tried to cast the Angel during the Declare Blockers step,
 *   the engine would allow the cast, but the Angel would enter the battlefield too late
 *   to block (blocking happens at the very start of the step).
 *
 * Rules Analysis: The Declare Attackers step includes a round of priority after attackers
 * are declared. This is the only window where a Flash creature can be cast to serve as a blocker.
 * The engine must decouple "Declaring Attackers" from "Ending the Step."
 */
class FlashAmbushTest : FunSpec({

    val player1Id = EntityId.of("player1")  // Player A (Attacking)
    val player2Id = EntityId.of("player2")  // Player B (Defending)

    // Grizzly Bears - 2/2 for {1}{G}
    val grizzlyBearsDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    // Restoration Angel - 3/4 Flying Flash for {3}{W}
    // "Flash. Flying. When Restoration Angel enters the battlefield, you may exile
    // target non-Angel creature you control, then return that card to the battlefield"
    // (We skip the ETB ability for this test - just testing Flash and blocking)
    val restorationAngelDef = CardDefinition.creature(
        name = "Restoration Angel",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype(value = "Angel")),
        power = 3,
        toughness = 4,
        keywords = setOf(Keyword.FLASH, Keyword.FLYING)
    )

    // Empty script repository (creatures don't need scripts for basic resolution)
    val scriptRepository = CardScriptRepository()

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    /**
     * Helper to add a creature to the battlefield.
     */
    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId,
        hasSummoningSickness: Boolean = true
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        if (hasSummoningSickness) {
            components.add(SummoningSicknessComponent)
        }
        val (creatureId, state1) = createEntity(EntityId.generate(), components)
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    /**
     * Helper to add a creature card to a player's hand.
     */
    fun GameState.addCreatureToHand(
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
     * Helper to give a player enough mana.
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
                    .addColorless(10)
            )
        }
    }

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
     * Helper to have a single player pass priority (non-advancing).
     */
    fun passPriority(state: GameState, playerId: EntityId): GameState {
        val result = GameEngine.executeAction(state, PassPriority(playerId))
        return (result as GameActionResult.Success).state
    }

    /**
     * Creates a game state at the Beginning of Combat step.
     */
    fun createGameAtBeginCombat(): GameState {
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

        // PRECOMBAT_MAIN - both pass
        state.turnState.step shouldBe Step.PRECOMBAT_MAIN
        state = passAndAdvance(state)

        // BEGIN_COMBAT
        state.turnState.step shouldBe Step.BEGIN_COMBAT
        return state
    }

    /**
     * Creates a game state at Declare Attackers step with combat started.
     */
    fun createGameAtDeclareAttackers(): GameState {
        var state = createGameAtBeginCombat()

        // Both pass in Begin Combat
        state = passAndAdvance(state)

        // DECLARE_ATTACKERS
        state.turnState.step shouldBe Step.DECLARE_ATTACKERS
        return state.startCombat(player2Id)
    }

    val handler = GameActionHandler()

    context("Scenario 3.2: The Flash Ambush (Declare Attackers Step)") {

        test("setup: Player A has Bears on battlefield, Player B has Restoration Angel in hand") {
            var state = createGameAtBeginCombat()

            // Add Grizzly Bears to Player A's battlefield (no summoning sickness - can attack)
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            state = state1

            // Add Restoration Angel to Player B's hand
            val (angelId, state2) = state.addCreatureToHand(restorationAngelDef, player2Id)
            state = state2

            // Give Player B mana
            state = state.giveInfiniteMana(player2Id)

            // Verify setup
            state.isOnBattlefield(bearsId).shouldBeTrue()
            state.getHand(player2Id) shouldContain angelId

            // Verify Restoration Angel has Flash
            val angelCard = state.getComponent<CardComponent>(angelId)
            angelCard.shouldNotBeNull()
            angelCard.definition.keywords shouldContain Keyword.FLASH
        }

        test("priority window exists after attackers are declared") {
            var state = createGameAtDeclareAttackers()

            // Add Bears (no summoning sickness) and declare as attacker
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            state = state1

            // Declare Bears as attacker
            val declareResult = handler.execute(state, DeclareAttacker(bearsId, player1Id))
            declareResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (declareResult as GameActionResult.Success).state

            // Bears should have AttackingComponent
            state.hasComponent<AttackingComponent>(bearsId).shouldBeTrue()

            // We're still in DECLARE_ATTACKERS step - priority window exists
            state.turnState.step shouldBe Step.DECLARE_ATTACKERS

            // Active player (Player A) has priority
            state.turnState.priorityPlayer shouldBe player1Id
        }

        test("Player B can cast Flash creature after attackers are declared") {
            var state = createGameAtDeclareAttackers()

            // Setup: Bears on battlefield, Angel in hand
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (angelId, state2) = state1.addCreatureToHand(restorationAngelDef, player2Id)
            state = state2.giveInfiniteMana(player2Id)

            // Declare Bears as attacker
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state

            // Player A passes priority
            state = passPriority(state, player1Id)

            // Priority should now be with Player B
            state.turnState.priorityPlayer shouldBe player2Id

            // Player B casts Restoration Angel (Flash allows casting at instant speed)
            val castResult = GameEngine.executeAction(
                state,
                CastSpell(
                    cardId = angelId,
                    casterId = player2Id,
                    fromZone = ZoneId.hand(player2Id)
                )
            )
            castResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (castResult as GameActionResult.Success).state

            // Angel should be on the stack
            state.getStack() shouldContain angelId
            state.getStack() shouldHaveSize 1
        }

        test("Flash creature resolves and can block in the same combat") {
            var state = createGameAtDeclareAttackers()

            // Setup
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (angelId, state2) = state1.addCreatureToHand(restorationAngelDef, player2Id)
            state = state2.giveInfiniteMana(player2Id)

            // === DECLARE ATTACKERS STEP ===
            // Declare Bears as attacker
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state

            // Player A passes priority
            state = passPriority(state, player1Id)

            // Player B casts Restoration Angel
            state = (GameEngine.executeAction(
                state,
                CastSpell(angelId, player2Id, ZoneId.hand(player2Id))
            ) as GameActionResult.Success).state

            // Both players pass priority, Angel resolves
            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = passPriority(state, player2Id)
            state = passPriority(state, player1Id)
            state.turnState.allPlayersPassed().shouldBeTrue()

            // Resolve Angel from stack
            val resolveResult = resolver.resolveTopOfStack(state)
            resolveResult.shouldBeInstanceOf<StackResolver.ResolutionResult.Resolved>()
            state = (resolveResult as StackResolver.ResolutionResult.Resolved).state

            // Angel should now be on the battlefield
            state.isOnBattlefield(angelId).shouldBeTrue()
            state.getStack().shouldBeEmpty()

            // Both players pass, move to DECLARE_BLOCKERS
            state = passAndAdvance(state)
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS

            // === DECLARE BLOCKERS STEP ===
            // Player B declares Restoration Angel as blocking Grizzly Bears
            val blockResult = handler.execute(state, DeclareBlocker(
                blockerId = angelId,
                attackerId = bearsId,
                controllerId = player2Id
            ))

            // The block should be legal
            blockResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (blockResult as GameActionResult.Success).state

            // Angel should have BlockingComponent
            state.hasComponent<BlockingComponent>(angelId).shouldBeTrue()
            val blocking = state.getComponent<BlockingComponent>(angelId)
            blocking.shouldNotBeNull()
            blocking.attackerId shouldBe bearsId

            // Bears should be blocked
            state.hasComponent<BlockedByComponent>(bearsId).shouldBeTrue()
        }

        test("complete Flash ambush scenario: Bears attacked, Angel blocks, Bears dies") {
            var state = createGameAtDeclareAttackers()

            // Setup
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (angelId, state2) = state1.addCreatureToHand(restorationAngelDef, player2Id)
            state = state2.giveInfiniteMana(player2Id)

            // === DECLARE ATTACKERS ===
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passPriority(state, player1Id)
            state = (GameEngine.executeAction(state, CastSpell(angelId, player2Id, ZoneId.hand(player2Id))) as GameActionResult.Success).state
            state = passPriority(state, player2Id)
            state = passPriority(state, player1Id)

            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state
            state = passAndAdvance(state)

            // === DECLARE BLOCKERS ===
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS
            state = (handler.execute(state, DeclareBlocker(angelId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state)

            // === FIRST STRIKE COMBAT DAMAGE (skipped if no first strikers) ===
            state.turnState.step shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state)

            // === COMBAT DAMAGE ===
            // Combat damage is automatically resolved as a turn-based action when entering this step
            state.turnState.step shouldBe Step.COMBAT_DAMAGE

            // Check damage marked
            // Bears (2/2) blocked by Angel (3/4)
            // Bears deals 2 to Angel, Angel deals 3 to Bears
            val bearsDamage = state.getComponent<DamageComponent>(bearsId)
            bearsDamage.shouldNotBeNull()
            bearsDamage.amount shouldBe 3  // Angel dealt 3 damage

            val angelDamage = state.getComponent<DamageComponent>(angelId)
            angelDamage.shouldNotBeNull()
            angelDamage.amount shouldBe 2  // Bears dealt 2 damage

            // SBA check
            state = (GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            // Bears should be dead (3 damage >= 2 toughness)
            state.isOnBattlefield(bearsId).shouldBeFalse()
            state.getGraveyard(player1Id) shouldContain bearsId

            // Angel should survive (2 damage < 4 toughness)
            state.isOnBattlefield(angelId).shouldBeTrue()
        }
    }

    context("Priority Window Verification") {

        test("engine does NOT auto-skip from Attackers to Blockers") {
            var state = createGameAtDeclareAttackers()

            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            state = state1

            // Declare attacker
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state

            // Still in DECLARE_ATTACKERS
            state.turnState.step shouldBe Step.DECLARE_ATTACKERS

            // Only after BOTH players pass should we advance
            state = passPriority(state, player1Id)
            state.turnState.step shouldBe Step.DECLARE_ATTACKERS  // Still here!

            state = passPriority(state, player2Id)
            state.turnState.allPlayersPassed().shouldBeTrue()
            state.turnState.step shouldBe Step.DECLARE_ATTACKERS  // Still here until resolved!

            // Now advance
            state = GameEngine.resolvePassedPriority(state)
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS
        }

        test("NAP gets priority to respond after attackers declared") {
            var state = createGameAtDeclareAttackers()

            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            state = state1

            // Declare attacker
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state

            // Active player (A) has priority first
            state.turnState.priorityPlayer shouldBe player1Id

            // A passes
            state = passPriority(state, player1Id)

            // Now NAP (B) has priority
            state.turnState.priorityPlayer shouldBe player2Id

            // B can take an action here (like casting Flash creature)
            // Priority has properly passed to the defending player
        }
    }

    context("Timing Constraint: Cast in Blockers Step") {

        test("creature cast during Declare Blockers enters too late to block") {
            var state = createGameAtDeclareAttackers()

            // Setup: Bears attacking, Angel in hand
            val (bearsId, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (angelId, state2) = state1.addCreatureToHand(restorationAngelDef, player2Id)
            state = state2.giveInfiniteMana(player2Id)

            // Declare attacker
            state = (handler.execute(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state

            // Both pass, move to Declare Blockers WITHOUT casting Angel
            // This transition captures the eligible blockers (creatures on battlefield NOW)
            state = passAndAdvance(state)
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS

            // Verify that eligible blockers were captured and Angel is NOT in the list
            // (because it wasn't on the battlefield when we entered DECLARE_BLOCKERS)
            state.combat.shouldNotBeNull()
            state.combat!!.eligibleBlockers.shouldNotBeNull()
            state.combat!!.eligibleBlockers!! shouldHaveSize 0  // No creatures on B's side yet

            // Now Player B tries to cast Angel during Declare Blockers
            // This is allowed (Flash lets you cast at instant speed)
            state = passPriority(state, player1Id)  // Active player passes first
            state = (GameEngine.executeAction(
                state,
                CastSpell(angelId, player2Id, ZoneId.hand(player2Id))
            ) as GameActionResult.Success).state

            // Angel is on the stack
            state.getStack() shouldContain angelId

            // Both pass, Angel resolves
            state = passPriority(state, player2Id)
            state = passPriority(state, player1Id)

            val resolver = StackResolver(scriptRepository = scriptRepository)
            state = (resolver.resolveTopOfStack(state) as StackResolver.ResolutionResult.Resolved).state

            // Angel is now on battlefield
            state.isOnBattlefield(angelId).shouldBeTrue()

            // BUT - blockers have already been declared (at the start of the step)
            // The Angel entered AFTER the eligible blockers were captured, so it cannot block
            // The engine tracks this via the eligibleBlockers set in CombatState

            // Verify Angel is NOT in the eligible blockers set
            state.combat!!.isEligibleBlocker(angelId).shouldBeFalse()

            // Attempting to declare it as a blocker should FAIL
            val blockResult = CombatValidator.canDeclareBlocker(
                state,
                blockerId = angelId,
                attackerId = bearsId,
                playerId = player2Id
            )

            // The engine correctly rejects the late blocker
            blockResult.shouldBeInstanceOf<CombatValidator.BlockValidationResult.Invalid>()
            (blockResult as CombatValidator.BlockValidationResult.Invalid).reason shouldBe
                "Creature entered the battlefield after blockers were declared and cannot block"
        }
    }

    context("Edge Cases") {

        test("multiple Flash creatures can be cast and block") {
            var state = createGameAtDeclareAttackers()

            // Create a second Flash creature
            val ambushViperDef = CardDefinition.creature(
                name = "Ambush Viper",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype(value = "Snake")),
                power = 2,
                toughness = 1,
                keywords = setOf(Keyword.FLASH, Keyword.DEATHTOUCH)
            )

            // Setup: Two attackers, two Flash creatures in hand
            val (bears1Id, state1) = state.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (bears2Id, state2) = state1.addCreatureToBattlefield(grizzlyBearsDef, player1Id, hasSummoningSickness = false)
            val (angelId, state3) = state2.addCreatureToHand(restorationAngelDef, player2Id)
            val (viperId, state4) = state3.addCreatureToHand(ambushViperDef, player2Id)
            state = state4.giveInfiniteMana(player2Id)

            // Declare both Bears as attackers
            state = (handler.execute(state, DeclareAttacker(bears1Id, player1Id)) as GameActionResult.Success).state
            state = (handler.execute(state, DeclareAttacker(bears2Id, player1Id)) as GameActionResult.Success).state

            // Player A passes
            state = passPriority(state, player1Id)

            // Player B casts both Flash creatures
            state = (GameEngine.executeAction(state, CastSpell(angelId, player2Id, ZoneId.hand(player2Id))) as GameActionResult.Success).state
            state = (GameEngine.executeAction(state, CastSpell(viperId, player2Id, ZoneId.hand(player2Id))) as GameActionResult.Success).state

            // Stack has both creatures (Viper on top)
            state.getStack() shouldHaveSize 2

            // Resolve both creatures - passAndAdvance resolves top of stack when stack isn't empty

            // First resolution: Viper (LIFO - last in, first out)
            state = passAndAdvance(state)  // Both pass, resolve top of stack
            // Note: passAndAdvance uses GameEngine.resolvePassedPriority which will
            // either advance step (if stack empty) or resolve stack (if not empty)

            // Since stack wasn't empty, it resolved Viper
            state.isOnBattlefield(viperId).shouldBeTrue()
            state.getStack() shouldHaveSize 1  // Angel still on stack

            // Second resolution: Angel
            state = passAndAdvance(state)

            // Angel resolved
            state.isOnBattlefield(angelId).shouldBeTrue()
            state.getStack().shouldBeEmpty()

            // Now both pass again to advance to Blockers step
            state = passAndAdvance(state)
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS

            // Both can block
            state = (handler.execute(state, DeclareBlocker(angelId, bears1Id, player2Id)) as GameActionResult.Success).state
            state = (handler.execute(state, DeclareBlocker(viperId, bears2Id, player2Id)) as GameActionResult.Success).state

            // Verify blocks
            state.hasComponent<BlockingComponent>(angelId).shouldBeTrue()
            state.hasComponent<BlockingComponent>(viperId).shouldBeTrue()
        }
    }
})
