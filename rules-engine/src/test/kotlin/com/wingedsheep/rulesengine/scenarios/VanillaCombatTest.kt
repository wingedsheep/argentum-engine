package com.wingedsheep.rulesengine.scenarios

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario 3.1: Vanilla Combat Exchange
 * Target System: Combat Math / Damage Marking
 * Complexity: Basic
 *
 * A simple creature vs. creature combat to ensure stats are compared
 * and damage is marked correctly without keywords.
 *
 * Setup:
 * - Attacker: Grizzly Bears (2/2) - Player A
 * - Blocker: Centaur Courser (3/3) - Player B
 *
 * Action Sequence:
 * 1. Declare Attackers: Player A attacks with Grizzly Bears
 * 2. Declare Blockers: Player B blocks with Centaur Courser
 * 3. Combat Damage: Players pass, engine resolves damage
 *
 * Expected Outcomes:
 * - Damage Marking: Grizzly Bears marks 3 damage, Centaur Courser marks 2 damage
 * - SBA Check: Grizzly Bears (toughness 2, damage 3) is destroyed
 *              Centaur Courser (toughness 3, damage 2) survives
 * - State: Grizzly Bears in Graveyard, Centaur Courser on Battlefield with 2 damage marked
 *
 * Rules Analysis: Verifies CR 510 (Combat Damage).
 * Ensures damage doesn't reduce toughness (a common misconception);
 * it is marked until end of turn.
 */
class VanillaCombatTest : FunSpec({

    val player1Id = _root_ide_package_.com.wingedsheep.rulesengine.ecs.EntityId.Companion.of("player1")  // Player A (Attacking)
    val player2Id = _root_ide_package_.com.wingedsheep.rulesengine.ecs.EntityId.Companion.of("player2")  // Player B (Blocking)

    // Grizzly Bears - 2/2 for {1}{G}
    val grizzlyBearsDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    // Centaur Courser - 3/3 for {2}{G}
    val centaurCourserDef = CardDefinition.creature(
        name = "Centaur Courser",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype(value = "Centaur"), Subtype(value = "Warrior")),
        power = 3,
        toughness = 3
    )

    fun newGame(): com.wingedsheep.rulesengine.ecs.GameState = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameState.Companion.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    /**
     * Helper to add a creature to the battlefield.
     */
    fun com.wingedsheep.rulesengine.ecs.GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: com.wingedsheep.rulesengine.ecs.EntityId,
        hasSummoningSickness: Boolean = true
    ): Pair<com.wingedsheep.rulesengine.ecs.EntityId, com.wingedsheep.rulesengine.ecs.GameState> {
        val components = mutableListOf<com.wingedsheep.rulesengine.ecs.Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        if (hasSummoningSickness) {
            components.add(SummoningSicknessComponent)
        }

        val (creatureId, state1) = createEntity(
            _root_ide_package_.com.wingedsheep.rulesengine.ecs.EntityId.Companion.generate(),
            components
        )
        return creatureId to state1.addToZone(creatureId, _root_ide_package_.com.wingedsheep.rulesengine.ecs.ZoneId.Companion.BATTLEFIELD)
    }

    /**
     * Helper to have both players pass priority and let the engine advance.
     * This is the proper way to test the engine - priority passing drives state transitions.
     */
    fun passAndAdvance(state: com.wingedsheep.rulesengine.ecs.GameState): com.wingedsheep.rulesengine.ecs.GameState {
        // Active player passes priority
        val result1 = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PassPriority(state.turnState.priorityPlayer))
        var currentState = (result1 as GameActionResult.Success).state

        // Non-active player passes priority
        val result2 = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(currentState, PassPriority(currentState.turnState.priorityPlayer))
        currentState = (result2 as GameActionResult.Success).state

        // All players have passed, engine resolves (advances step or resolves stack)
        currentState.turnState.allPlayersPassed().shouldBeTrue()
        return _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.resolvePassedPriority(currentState)
    }

    /**
     * Creates a game state ready for the declare attackers step with combat started.
     * Uses proper priority passing to advance through the turn.
     */
    fun createGameInDeclareAttackersStep(): com.wingedsheep.rulesengine.ecs.GameState {
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

        // PRECOMBAT_MAIN - both players pass
        state.turnState.step shouldBe Step.PRECOMBAT_MAIN
        state = passAndAdvance(state)

        // BEGIN_COMBAT - both players pass
        state.turnState.step shouldBe Step.BEGIN_COMBAT
        state = passAndAdvance(state)

        // Now at DECLARE_ATTACKERS
        state.turnState.step shouldBe Step.DECLARE_ATTACKERS
        return state.startCombat(player2Id)
    }

    val handler = GameActionHandler()

    context("Scenario 3.1: Vanilla Combat Exchange") {

        test("setup: Grizzly Bears (2/2) attacks, Centaur Courser (3/3) blocks") {
            var state = createGameInDeclareAttackersStep()

            // Add Grizzly Bears for Player A (attacker) - no summoning sickness
            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )

            // Add Centaur Courser for Player B (blocker)
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Verify creatures are on battlefield
            state.isOnBattlefield(bearsId).shouldBeTrue()
            state.isOnBattlefield(courserId).shouldBeTrue()

            // Verify stats
            val bearsCard = state.getComponent<CardComponent>(bearsId)!!
            bearsCard.definition.creatureStats!!.basePower shouldBe 2
            bearsCard.definition.creatureStats!!.baseToughness shouldBe 2

            val courserCard = state.getComponent<CardComponent>(courserId)!!
            courserCard.definition.creatureStats!!.basePower shouldBe 3
            courserCard.definition.creatureStats!!.baseToughness shouldBe 3
        }

        test("complete combat flow: attack, block, damage resolution, state-based actions") {
            // === SETUP ===
            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // === STEP 1: DECLARE ATTACKERS ===
            state.turnState.step shouldBe Step.DECLARE_ATTACKERS

            // Player A attacks with Grizzly Bears
            val declareAttackerResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id))
            declareAttackerResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (declareAttackerResult as GameActionResult.Success).state

            // Verify Bears is now attacking
            state.hasComponent<AttackingComponent>(bearsId).shouldBeTrue()
            val attackingComponent = state.getComponent<AttackingComponent>(bearsId)!!
            attackingComponent.target.shouldBeInstanceOf<CombatTarget.Player>()
            (attackingComponent.target as CombatTarget.Player).playerId shouldBe player2Id

            // Bears should be tapped (no vigilance)
            state.hasComponent<TappedComponent>(bearsId).shouldBeTrue()

            // Both players pass priority to advance to declare blockers
            state = passAndAdvance(state)

            // === STEP 2: DECLARE BLOCKERS ===
            state.turnState.step shouldBe Step.DECLARE_BLOCKERS

            // Player B blocks with Centaur Courser
            val declareBlockerResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(courserId, bearsId, player2Id))
            declareBlockerResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (declareBlockerResult as GameActionResult.Success).state

            // Verify Courser is blocking Bears
            state.hasComponent<BlockingComponent>(courserId).shouldBeTrue()
            val blockingComponent = state.getComponent<BlockingComponent>(courserId)!!
            blockingComponent.attackerId shouldBe bearsId

            // Verify Bears knows it's blocked
            state.hasComponent<BlockedByComponent>(bearsId).shouldBeTrue()
            val blockedBy = state.getComponent<BlockedByComponent>(bearsId)!!
            blockedBy.isBlocked.shouldBeTrue()
            blockedBy.blockerIds.contains(courserId).shouldBeTrue()

            // Both players pass priority to advance to first strike damage
            state = passAndAdvance(state)

            // === STEP 3: FIRST STRIKE COMBAT DAMAGE ===
            state.turnState.step shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE

            // No first strike creatures, both players pass
            state = passAndAdvance(state)

            // === STEP 4: COMBAT DAMAGE ===
            state.turnState.step shouldBe Step.COMBAT_DAMAGE

            // Resolve combat damage (regular step - no first strike creatures)
            val combatDamageResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(
                state,
                ResolveCombatDamage(CombatDamageStep.REGULAR)
            )
            combatDamageResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (combatDamageResult as GameActionResult.Success).state

            // === VERIFY DAMAGE MARKING ===
            // Grizzly Bears (2/2) took 3 damage from Centaur Courser (3 power)
            val bearsDamage = state.getComponent<DamageComponent>(bearsId)
            bearsDamage.shouldNotBeNull()
            bearsDamage.amount shouldBe 3

            // Centaur Courser (3/3) took 2 damage from Grizzly Bears (2 power)
            val courserDamage = state.getComponent<DamageComponent>(courserId)
            courserDamage.shouldNotBeNull()
            courserDamage.amount shouldBe 2

            // === VERIFY DAMAGE DOESN'T REDUCE TOUGHNESS ===
            // This is a common misconception - damage is marked, not subtracted from toughness
            val bearsCard = state.getComponent<CardComponent>(bearsId)!!
            bearsCard.definition.creatureStats!!.baseToughness shouldBe 2  // Still 2

            val courserCard = state.getComponent<CardComponent>(courserId)!!
            courserCard.definition.creatureStats!!.baseToughness shouldBe 3  // Still 3

            // === STEP 5: STATE-BASED ACTIONS ===
            // Check state-based actions - creatures with lethal damage die
            val sbaResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions())
            sbaResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (sbaResult as GameActionResult.Success).state

            // === VERIFY FINAL STATE ===
            // Grizzly Bears (toughness 2, damage 3) should be destroyed
            // 3 >= 2, so lethal damage
            state.isOnBattlefield(bearsId).shouldBeFalse()
            state.getGraveyard(player1Id).contains(bearsId).shouldBeTrue()

            // Centaur Courser (toughness 3, damage 2) survives
            // 2 < 3, so not lethal
            state.isOnBattlefield(courserId).shouldBeTrue()
            state.getGraveyard(player2Id).contains(courserId).shouldBeFalse()

            // Courser still has 2 damage marked (until cleanup step)
            val courserFinalDamage = state.getComponent<DamageComponent>(courserId)
            courserFinalDamage.shouldNotBeNull()
            courserFinalDamage.amount shouldBe 2
        }

        test("damage events are generated correctly during combat damage") {
            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Declare attacker
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS

            // Declare blocker
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(courserId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE

            state.turnState.step shouldBe Step.COMBAT_DAMAGE

            // Resolve combat damage and check events
            val combatDamageResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(
                state,
                ResolveCombatDamage(CombatDamageStep.REGULAR)
            ) as GameActionResult.Success

            // Should have damage dealt events
            val damageEvents = combatDamageResult.events.filterIsInstance<GameActionEvent.DamageDealt>()
            damageEvents.size shouldBe 2

            // Bears deals 2 to Courser
            val bearsDamageEvent = damageEvents.find { it.sourceId == bearsId }
            bearsDamageEvent.shouldNotBeNull()
            bearsDamageEvent.targetId shouldBe courserId
            bearsDamageEvent.amount shouldBe 2
            bearsDamageEvent.isCombatDamage.shouldBeTrue()

            // Courser deals 3 to Bears
            val courserDamageEvent = damageEvents.find { it.sourceId == courserId }
            courserDamageEvent.shouldNotBeNull()
            courserDamageEvent.targetId shouldBe bearsId
            courserDamageEvent.amount shouldBe 3
            courserDamageEvent.isCombatDamage.shouldBeTrue()
        }

        test("CreatureDied event is generated when Bears dies to lethal damage") {
            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Full combat flow with priority passing
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(courserId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResolveCombatDamage(CombatDamageStep.REGULAR)) as GameActionResult.Success).state

            // Check SBA and verify CreatureDied event
            val sbaResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success

            val creatureDiedEvents = sbaResult.events.filterIsInstance<GameActionEvent.CreatureDied>()
            creatureDiedEvents.size shouldBe 1

            val bearsDiedEvent = creatureDiedEvents.first()
            bearsDiedEvent.entityId shouldBe bearsId
            bearsDiedEvent.name shouldBe "Grizzly Bears"
            bearsDiedEvent.ownerId shouldBe player1Id
        }

        test("combat ends and removes combat components") {
            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Full combat until damage resolved
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(courserId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResolveCombatDamage(CombatDamageStep.REGULAR)) as GameActionResult.Success).state
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            // Combat is still active
            state.combat.shouldNotBeNull()

            // End combat
            val endCombatResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, EndCombat(player1Id))
            endCombatResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (endCombatResult as GameActionResult.Success).state

            // Combat state should be cleared
            state.combat.shouldBeNull()

            // Courser should no longer have BlockingComponent (Bears is dead, so no AttackingComponent to check)
            state.hasComponent<BlockingComponent>(courserId).shouldBeFalse()

            // Courser still has damage marked (damage clears in cleanup, not end of combat)
            state.getComponent<DamageComponent>(courserId)!!.amount shouldBe 2
        }
    }

    context("CR 510 - Combat Damage Rules Verification") {

        test("damage is simultaneous for all creatures") {
            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,
                player1Id,
                hasSummoningSickness = false
            )
            val (courserId, state2) = state1.addCreatureToBattlefield(
                centaurCourserDef,
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Setup combat with proper priority passing
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(courserId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE

            state.turnState.step shouldBe Step.COMBAT_DAMAGE

            // Before damage, both have no damage
            state.getComponent<DamageComponent>(bearsId).shouldBeNull()
            state.getComponent<DamageComponent>(courserId).shouldBeNull()

            // Resolve damage - a single action applies all damage
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResolveCombatDamage(CombatDamageStep.REGULAR)) as GameActionResult.Success).state

            // After single damage resolution, BOTH creatures have damage marked
            // This proves damage is simultaneous (not sequential)
            state.getComponent<DamageComponent>(bearsId)!!.amount shouldBe 3
            state.getComponent<DamageComponent>(courserId)!!.amount shouldBe 2

            // Both are still on battlefield until SBA check
            state.isOnBattlefield(bearsId).shouldBeTrue()
            state.isOnBattlefield(courserId).shouldBeTrue()
        }

        test("lethal damage threshold is damage >= toughness") {
            var state = createGameInDeclareAttackersStep()

            // Create a 3/3 creature
            val (giantId, state1) = state.addCreatureToBattlefield(
                centaurCourserDef,  // 3/3
                player1Id,
                hasSummoningSickness = false
            )
            state = state1

            // Mark exactly lethal damage (3 damage on a 3 toughness creature)
            state = state.updateEntity(giantId) { it.with(DamageComponent(3)) }

            // Check SBA - should die (3 >= 3)
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            state.isOnBattlefield(giantId).shouldBeFalse()
            state.getGraveyard(player1Id).contains(giantId).shouldBeTrue()
        }

        test("damage below toughness does not kill") {
            var state = createGameInDeclareAttackersStep()

            // Create a 3/3 creature
            val (giantId, state1) = state.addCreatureToBattlefield(
                centaurCourserDef,  // 3/3
                player1Id,
                hasSummoningSickness = false
            )
            state = state1

            // Mark 2 damage (less than 3 toughness)
            state = state.updateEntity(giantId) { it.with(DamageComponent(2)) }

            // Check SBA - should survive (2 < 3)
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            state.isOnBattlefield(giantId).shouldBeTrue()
            state.getComponent<DamageComponent>(giantId)!!.amount shouldBe 2
        }

        test("damage persists until cleanup step") {
            var state = createGameInDeclareAttackersStep()

            val (courserId, state1) = state.addCreatureToBattlefield(
                centaurCourserDef,  // 3/3
                player1Id,
                hasSummoningSickness = false
            )
            state = state1

            // Mark 2 damage
            state = state.updateEntity(courserId) { it.with(DamageComponent(2)) }

            // Advance through combat phase with priority passing
            state = passAndAdvance(state) // DECLARE_ATTACKERS -> DECLARE_BLOCKERS
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE
            state = passAndAdvance(state) // -> END_COMBAT
            state = passAndAdvance(state) // -> POSTCOMBAT_MAIN

            // Damage should persist through combat
            state.getComponent<DamageComponent>(courserId)!!.amount shouldBe 2

            // Advance to end step
            state = passAndAdvance(state) // POSTCOMBAT_MAIN -> END
            state.turnState.step shouldBe Step.END
            state.getComponent<DamageComponent>(courserId)!!.amount shouldBe 2

            // Advance to cleanup step (no priority in cleanup, use direct advance)
            state = passAndAdvance(state) // END -> CLEANUP
            state.turnState.step shouldBe Step.CLEANUP
            state.getComponent<DamageComponent>(courserId)!!.amount shouldBe 2

            // Execute cleanup action to clear damage
            val cleanupResult = _root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, PerformCleanupStep(player1Id))
            cleanupResult.shouldBeInstanceOf<GameActionResult.Success>()
            state = (cleanupResult as GameActionResult.Success).state

            // Damage should be cleared
            state.getComponent<DamageComponent>(courserId).shouldBeNull()
        }
    }

    context("edge cases") {

        test("zero power creature deals no damage") {
            // Create a 0/3 creature definition (toughness 3 to survive 2 damage from Bears)
            val wallDef = CardDefinition.creature(
                name = "Wall of Frost",
                manaCost = ManaCost.parse("{1}{U}{U}"),
                subtypes = setOf(Subtype(value = "Wall")),
                power = 0,
                toughness = 3
            )

            var state = createGameInDeclareAttackersStep()

            val (bearsId, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,  // 2/2
                player1Id,
                hasSummoningSickness = false
            )
            val (wallId, state2) = state1.addCreatureToBattlefield(
                wallDef,  // 0/3
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Attack and block with proper priority passing
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bearsId, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(wallId, bearsId, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE

            // Resolve damage
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResolveCombatDamage(CombatDamageStep.REGULAR)) as GameActionResult.Success).state

            // Wall (0 power) deals no damage to Bears
            state.getComponent<DamageComponent>(bearsId).shouldBeNull()

            // Bears (2 power) deals 2 damage to Wall
            state.getComponent<DamageComponent>(wallId)!!.amount shouldBe 2

            // SBA check - both survive (wall has 3 toughness with 2 damage, Bears has no damage)
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            state.isOnBattlefield(bearsId).shouldBeTrue()
            state.isOnBattlefield(wallId).shouldBeTrue()
        }

        test("mutual destruction when both have lethal damage") {
            // Create two 2/2 creatures
            var state = createGameInDeclareAttackersStep()

            val (bears1Id, state1) = state.addCreatureToBattlefield(
                grizzlyBearsDef,  // 2/2
                player1Id,
                hasSummoningSickness = false
            )
            val (bears2Id, state2) = state1.addCreatureToBattlefield(
                grizzlyBearsDef,  // 2/2
                player2Id,
                hasSummoningSickness = false
            )
            state = state2

            // Attack and block with proper priority passing
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareAttacker(bears1Id, player1Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> DECLARE_BLOCKERS
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, DeclareBlocker(bears2Id, bears1Id, player2Id)) as GameActionResult.Success).state
            state = passAndAdvance(state) // -> FIRST_STRIKE_COMBAT_DAMAGE
            state = passAndAdvance(state) // -> COMBAT_DAMAGE

            // Resolve damage
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, ResolveCombatDamage(CombatDamageStep.REGULAR)) as GameActionResult.Success).state

            // Both deal 2 damage to each other (2/2 vs 2/2)
            state.getComponent<DamageComponent>(bears1Id)!!.amount shouldBe 2
            state.getComponent<DamageComponent>(bears2Id)!!.amount shouldBe 2

            // SBA check - both die (2 >= 2)
            state = (_root_ide_package_.com.wingedsheep.rulesengine.ecs.GameEngine.executeAction(state, CheckStateBasedActions()) as GameActionResult.Success).state

            state.isOnBattlefield(bears1Id).shouldBeFalse()
            state.isOnBattlefield(bears2Id).shouldBeFalse()

            state.getGraveyard(player1Id).contains(bears1Id).shouldBeTrue()
            state.getGraveyard(player2Id).contains(bears2Id).shouldBeTrue()
        }
    }
})
