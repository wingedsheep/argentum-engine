package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * Type of stack item for test mocking.
 */
enum class StackItemType { ABILITY, PERMANENT_SPELL, INSTANT_SORCERY }

/**
 * Tests for the Arena-style AutoPassManager.
 *
 * These tests verify the 4 Rules:
 * - Rule 1: Meaningful Action Filter
 * - Rule 2: Opponent's Turn Compression
 * - Rule 3: My Turn Optimization
 * - Rule 4: Stack Response (Absolute Rule)
 */
class AutoPassManagerTest : FunSpec({
    val autoPassManager = AutoPassManager()
    val player1 = EntityId.generate()
    val player2 = EntityId.generate()

    fun createMockState(
        priorityPlayerId: EntityId?,
        activePlayerId: EntityId,
        step: Step,
        stackEmpty: Boolean = true,
        hasPendingDecision: Boolean = false,
        stackControllerId: EntityId? = null,
        stackItemType: StackItemType = StackItemType.ABILITY,
        blockersHaveBeenDeclared: Boolean = false,
        defendingPlayerId: EntityId? = null
    ): GameState {
        val state = mockk<GameState>(relaxed = true)
        every { state.priorityPlayerId } returns priorityPlayerId
        every { state.activePlayerId } returns activePlayerId
        every { state.step } returns step
        every { state.phase } returns step.phase

        // Set up turnOrder for blocker detection
        val defender = defendingPlayerId ?: if (activePlayerId == player1) player2 else player1
        every { state.turnOrder } returns listOf(activePlayerId, defender)

        // Mock BlockersDeclaredThisCombatComponent on defender if blockers have been declared
        if (blockersHaveBeenDeclared) {
            val defenderEntity = mockk<com.wingedsheep.engine.state.ComponentContainer>(relaxed = true)
            every { defenderEntity.get<BlockersDeclaredThisCombatComponent>() } returns BlockersDeclaredThisCombatComponent
            every { state.getEntity(defender) } returns defenderEntity
        }

        if (stackEmpty) {
            every { state.stack } returns emptyList()
        } else {
            val stackItemId = EntityId.generate()
            every { state.stack } returns listOf(stackItemId)

            // Mock the stack item entity with appropriate component
            val stackEntity = mockk<com.wingedsheep.engine.state.ComponentContainer>(relaxed = true)
            every { state.getEntity(stackItemId) } returns stackEntity

            val controllerId = stackControllerId ?: activePlayerId

            when (stackItemType) {
                StackItemType.ABILITY -> {
                    val abilityComponent = com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent(
                        sourceId = EntityId.generate(),
                        sourceName = "Test Ability",
                        controllerId = controllerId,
                        effect = mockk(relaxed = true)
                    )
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>() } returns abilityComponent
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>() } returns null
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>() } returns null
                }
                StackItemType.PERMANENT_SPELL -> {
                    val spellComponent = com.wingedsheep.engine.state.components.stack.SpellOnStackComponent(
                        casterId = controllerId
                    )
                    val cardComponent = mockk<com.wingedsheep.engine.state.components.identity.CardComponent>(relaxed = true)
                    every { cardComponent.isPermanent } returns true
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>() } returns null
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>() } returns null
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>() } returns spellComponent
                    every { stackEntity.get<com.wingedsheep.engine.state.components.identity.CardComponent>() } returns cardComponent
                }
                StackItemType.INSTANT_SORCERY -> {
                    val spellComponent = com.wingedsheep.engine.state.components.stack.SpellOnStackComponent(
                        casterId = controllerId
                    )
                    val cardComponent = mockk<com.wingedsheep.engine.state.components.identity.CardComponent>(relaxed = true)
                    every { cardComponent.isPermanent } returns false
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>() } returns null
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>() } returns null
                    every { stackEntity.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>() } returns spellComponent
                    every { stackEntity.get<com.wingedsheep.engine.state.components.identity.CardComponent>() } returns cardComponent
                }
            }
            every { stackEntity.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>() } returns null
        }

        every { state.pendingDecision } returns if (hasPendingDecision) mockk() else null
        every { state.gameOver } returns false
        return state
    }

    fun passPriorityAction(playerId: EntityId) = LegalActionInfo(
        actionType = "PassPriority",
        description = "Pass priority",
        action = PassPriority(playerId)
    )

    fun manaAbilityAction(playerId: EntityId) = LegalActionInfo(
        actionType = "ActivateAbility",
        description = "Tap for mana",
        action = ActivateAbility(playerId, EntityId.generate(), AbilityId("mana")),
        isManaAbility = true
    )

    fun instantSpellAction(playerId: EntityId, hasTargets: Boolean = true) = LegalActionInfo(
        actionType = "CastSpell",
        description = "Cast Lightning Bolt",
        action = CastSpell(playerId, EntityId.generate()),
        requiresTargets = true,
        validTargets = if (hasTargets) listOf(EntityId.generate()) else emptyList()
    )

    fun nonTargetedSpellAction(playerId: EntityId) = LegalActionInfo(
        actionType = "CastSpell",
        description = "Cast Giant Growth",
        action = CastSpell(playerId, EntityId.generate()),
        requiresTargets = false
    )

    fun playLandAction(playerId: EntityId) = LegalActionInfo(
        actionType = "PlayLand",
        description = "Play Forest",
        action = PlayLand(playerId, EntityId.generate())
    )

    context("Rule 1: Meaningful Action Filter") {
        test("PassPriority is not a meaningful action") {
            // Use a non-main-phase step to test meaningful action filtering
            val state = createMockState(player1, player1, Step.END_COMBAT)
            val actions = listOf(passPriorityAction(player1))

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Mana abilities are not meaningful actions") {
            val state = createMockState(player1, player1, Step.END_COMBAT)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Spells with no valid targets are not meaningful") {
            val state = createMockState(player1, player1, Step.END_COMBAT)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1, hasTargets = false) // No valid targets
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Spells with valid targets are meaningful") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1, hasTargets = true)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("Non-targeted spells are meaningful") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player1),
                nonTargetedSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("PlayLand is meaningful") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player1),
                playLandAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }
    }

    context("Rule 2: Opponent's Turn Compression (Arena-style aggressive passing)") {
        // Arena auto-passes through most of opponent's turn.
        // Only stops at: declare blockers (if you have blockers), end step (if you have instants)

        test("Auto-pass during opponent's upkeep even with instants") {
            val state = createMockState(player2, player1, Step.UPKEEP)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's draw step even with instants") {
            val state = createMockState(player2, player1, Step.DRAW)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's main phase even with instants") {
            val state = createMockState(player2, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's begin combat even with instants (Arena-style)") {
            val state = createMockState(player2, player1, Step.BEGIN_COMBAT)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("STOP during opponent's declare attackers when have instant-speed responses") {
            // This is important for cards like Blessed Reversal and Scorching Winds
            // that can ONLY be cast during the declare attackers step
            val state = createMockState(player2, player1, Step.DECLARE_ATTACKERS)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("Auto-pass during opponent's declare attackers with no instant-speed responses") {
            val state = createMockState(player2, player1, Step.DECLARE_ATTACKERS)
            val actions = listOf(
                passPriorityAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during declare blockers when only instants available (no blockers)") {
            val state = createMockState(player2, player1, Step.DECLARE_BLOCKERS)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2) // Has instant but no blockers
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("STOP during declare blockers when blockers available") {
            val state = createMockState(player2, player1, Step.DECLARE_BLOCKERS)
            val blockerId = EntityId.generate()
            val attackerId = EntityId.generate()
            val actions = listOf(
                passPriorityAction(player2),
                LegalActionInfo(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = PassPriority(player2), // placeholder - action type is what matters
                    validBlockers = listOf(blockerId),
                    validAttackers = listOf(attackerId)
                )
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("Auto-pass during opponent's combat damage even with instants (Arena-style)") {
            val state = createMockState(player2, player1, Step.COMBAT_DAMAGE)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("STOP during opponent's end step when you have instant-speed actions") {
            val state = createMockState(player2, player1, Step.END)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("Auto-pass during opponent's end step when no instant-speed actions") {
            val state = createMockState(player2, player1, Step.END)
            val actions = listOf(
                passPriorityAction(player2),
                manaAbilityAction(player2) // Only mana ability
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }
    }

    context("Rule 3: My Turn Optimization (Arena-style aggressive passing)") {
        // Arena only stops at main phases and declare attackers on your own turn.
        // Everything else auto-passes, even if you have instant-speed actions.

        test("Auto-pass during my upkeep even with instants") {
            val state = createMockState(player1, player1, Step.UPKEEP)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my draw step even with instants") {
            val state = createMockState(player1, player1, Step.DRAW)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("STOP during my main phase") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("STOP during my declare attackers") {
            val state = createMockState(player1, player1, Step.DECLARE_ATTACKERS)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("Auto-pass during my declare blockers even with combat tricks (Arena-style)") {
            // Arena auto-passes here - if you want to use combat tricks, use Full Control
            val state = createMockState(player1, player1, Step.DECLARE_BLOCKERS)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my first strike damage even with instants (Arena-style)") {
            val state = createMockState(player1, player1, Step.FIRST_STRIKE_COMBAT_DAMAGE)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my combat damage even with instants (Arena-style)") {
            val state = createMockState(player1, player1, Step.COMBAT_DAMAGE)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my end combat even with instants (Arena-style)") {
            val state = createMockState(player1, player1, Step.END_COMBAT)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my end step even with instants (Arena-style)") {
            val state = createMockState(player1, player1, Step.END)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }
    }

    context("Rule 4: Stack Response - Arena Style") {
        test("AUTO-PASS when own ability is on stack") {
            // player1's ability on stack, player1 has priority → auto-pass to let opponent respond
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player1)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("STOP when opponent's ability is on stack") {
            // player2's ability on stack, player1 has priority → stop to consider response
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player2)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("STOP when opponent's ability is on stack even during opponent's main") {
            // player1's ability on stack (opponent from player2's perspective), player2 has priority
            val state = createMockState(player2, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player1)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("AUTO-PASS when own ability is on stack even with no responses available") {
            // player1's ability on stack, player1 has priority → auto-pass
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player1)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1) // Only mana ability, not a response
            )

            // Auto-pass when own ability is on stack (let opponent respond)
            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("STOP when opponent's ability is on stack with no responses") {
            // player2's ability on stack, player1 has priority but no responses → stop to see the effect
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player2, stackItemType = StackItemType.ABILITY)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1) // Only mana ability, not a response
            )

            // Stop so player can see the opponent's ability effect
            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("STOP when opponent's instant/sorcery is on stack with no responses") {
            // player2's instant on stack, player1 has priority but no responses → stop to see the effect
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player2, stackItemType = StackItemType.INSTANT_SORCERY)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1) // Only mana ability, not a response
            )

            // Stop so player can see the opponent's spell effect
            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("AUTO-PASS when opponent's permanent spell is on stack with no responses") {
            // player2's creature on stack, player1 has priority but no responses → auto-pass
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player2, stackItemType = StackItemType.PERMANENT_SPELL)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1) // Only mana ability, not a response
            )

            // Auto-pass since permanent just enters the battlefield
            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }
    }

    context("Edge Cases") {
        test("Never auto-pass when there's a pending decision") {
            val state = createMockState(player1, player1, Step.UPKEEP, hasPendingDecision = true)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("Cannot auto-pass if player doesn't have priority") {
            val state = createMockState(player2, player1, Step.PRECOMBAT_MAIN) // player2 has priority
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("Auto-pass with only PassPriority available (non-main phase)") {
            val state = createMockState(player1, player1, Step.END_COMBAT)
            val actions = listOf(passPriorityAction(player1))

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Never auto-pass own main phase even with no meaningful actions") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(passPriorityAction(player1))

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }
    }

    context("Declare Blockers Priority Window (combat tricks)") {
        test("STOP during my declare blockers step after blockers declared if I have combat tricks") {
            // After opponent declares blockers, attacking player should get chance to cast Giant Growth
            val state = createMockState(
                priorityPlayerId = player1,
                activePlayerId = player1,
                step = Step.DECLARE_BLOCKERS,
                blockersHaveBeenDeclared = true,
                defendingPlayerId = player2
            )
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)  // Combat trick like Giant Growth
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("Auto-pass during my declare blockers step after blockers declared if NO instant-speed actions") {
            // If attacker has no combat tricks, auto-pass to damage step
            val state = createMockState(
                priorityPlayerId = player1,
                activePlayerId = player1,
                step = Step.DECLARE_BLOCKERS,
                blockersHaveBeenDeclared = true,
                defendingPlayerId = player2
            )
            val actions = listOf(
                passPriorityAction(player1)
                // No instant-speed actions
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("STOP during opponent's declare blockers step after blockers declared if I have combat tricks") {
            // After declaring blockers, defending player should also get a chance to cast instants
            val state = createMockState(
                priorityPlayerId = player2,
                activePlayerId = player1,  // It's player1's turn (attacker)
                step = Step.DECLARE_BLOCKERS,
                blockersHaveBeenDeclared = true,
                defendingPlayerId = player2
            )
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)  // Combat trick
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("Auto-pass during my declare blockers step BEFORE blockers declared even with combat tricks") {
            // Before blockers are declared, attacker should auto-pass (Arena-style)
            val state = createMockState(
                priorityPlayerId = player1,
                activePlayerId = player1,
                step = Step.DECLARE_BLOCKERS,
                blockersHaveBeenDeclared = false  // Not yet declared
            )
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }
    }

    context("getNextStopPoint - button labels") {
        test("returns Resolve when stack is non-empty") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false, stackControllerId = player1)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "Resolve"
        }

        test("returns 'Pass to Main 2' from declare attackers on my turn") {
            // On my turn at declare attackers with no attackers, next stop is postcombat main
            val state = createMockState(player1, player1, Step.DECLARE_ATTACKERS)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "Pass to Main 2"
        }

        test("returns 'End Turn' from postcombat main on my turn") {
            val state = createMockState(player1, player1, Step.POSTCOMBAT_MAIN)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "End Turn"
        }

        test("returns 'To my turn' from opponent's end step with meaningful actions") {
            val state = createMockState(player1, player2, Step.END)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "To my turn"
        }

        test("returns 'Pass to Main' from my end step without meaningful actions") {
            // Without meaningful actions, skips through opponent's entire turn back to my main phase
            val state = createMockState(player1, player1, Step.END)
            autoPassManager.getNextStopPoint(state, player1, false) shouldBe "Pass to Main"
        }

        test("returns 'Pass to End Step' from declare attackers on opponent's turn with no attackers") {
            // With no attacking creatures, engine skips blockers and combat damage steps (CR 508.8)
            val state = createMockState(player1, player2, Step.DECLARE_ATTACKERS)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "Pass to End Step"
        }

        test("returns 'Pass to End Step' from opponent's combat with meaningful actions") {
            val state = createMockState(player1, player2, Step.COMBAT_DAMAGE)
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "Pass to End Step"
        }
    }

    context("getNextStopPoint - combat damage labels") {
        fun createCombatState(
            step: Step,
            attackerIds: List<EntityId>,
            blockerIds: List<EntityId> = emptyList(),
            firstStrikeEntityIds: Set<EntityId> = emptySet(),
            doubleStrikeEntityIds: Set<EntityId> = emptySet()
        ): Pair<GameState, StateProjector> {
            val state = mockk<GameState>(relaxed = true)
            every { state.priorityPlayerId } returns player1
            every { state.activePlayerId } returns player1
            every { state.step } returns step
            every { state.phase } returns step.phase
            every { state.stack } returns emptyList()
            every { state.pendingDecision } returns null
            every { state.turnOrder } returns listOf(player1, player2)

            // Set up battlefield with attackers and blockers
            val allEntities = attackerIds + blockerIds
            every { state.getBattlefield() } returns allEntities

            for (attackerId in attackerIds) {
                val entity = mockk<com.wingedsheep.engine.state.ComponentContainer>(relaxed = true)
                every { entity.get<AttackingComponent>() } returns AttackingComponent(player2)
                every { entity.get<BlockingComponent>() } returns null
                every { state.getEntity(attackerId) } returns entity
            }

            for (blockerId in blockerIds) {
                val entity = mockk<com.wingedsheep.engine.state.ComponentContainer>(relaxed = true)
                every { entity.get<AttackingComponent>() } returns null
                every { entity.get<BlockingComponent>() } returns BlockingComponent(attackerIds)
                every { state.getEntity(blockerId) } returns entity
            }

            // Set up state projector mock
            val stateProjector = mockk<StateProjector>()
            val projected = mockk<ProjectedState>()
            every { stateProjector.project(state) } returns projected

            for (entityId in allEntities) {
                every { projected.hasKeyword(entityId, Keyword.FIRST_STRIKE) } returns (entityId in firstStrikeEntityIds)
                every { projected.hasKeyword(entityId, Keyword.DOUBLE_STRIKE) } returns (entityId in doubleStrikeEntityIds)
            }

            return state to stateProjector
        }

        test("at DECLARE_BLOCKERS with attackers and no first strike returns 'Resolve combat damage'") {
            val attacker = EntityId.generate()
            val (state, projector) = createCombatState(
                step = Step.DECLARE_BLOCKERS,
                attackerIds = listOf(attacker)
            )
            autoPassManager.getNextStopPoint(state, player1, true, projector) shouldBe "Resolve combat damage"
        }

        test("at DECLARE_BLOCKERS with first strike attacker returns 'Resolve first strike damage'") {
            val attacker = EntityId.generate()
            val (state, projector) = createCombatState(
                step = Step.DECLARE_BLOCKERS,
                attackerIds = listOf(attacker),
                firstStrikeEntityIds = setOf(attacker)
            )
            autoPassManager.getNextStopPoint(state, player1, true, projector) shouldBe "Resolve first strike damage"
        }

        test("at DECLARE_BLOCKERS with double strike blocker returns 'Resolve first strike damage'") {
            val attacker = EntityId.generate()
            val blocker = EntityId.generate()
            val (state, projector) = createCombatState(
                step = Step.DECLARE_BLOCKERS,
                attackerIds = listOf(attacker),
                blockerIds = listOf(blocker),
                doubleStrikeEntityIds = setOf(blocker)
            )
            autoPassManager.getNextStopPoint(state, player1, true, projector) shouldBe "Resolve first strike damage"
        }

        test("at FIRST_STRIKE_COMBAT_DAMAGE with attackers returns 'Resolve combat damage'") {
            val attacker = EntityId.generate()
            val (state, _) = createCombatState(
                step = Step.FIRST_STRIKE_COMBAT_DAMAGE,
                attackerIds = listOf(attacker)
            )
            autoPassManager.getNextStopPoint(state, player1, true) shouldBe "Resolve combat damage"
        }

        test("at DECLARE_BLOCKERS with no attackers falls through to normal logic") {
            // No attackers on battlefield - should not return combat label
            val state = createMockState(player1, player1, Step.DECLARE_BLOCKERS)
            val result = autoPassManager.getNextStopPoint(state, player1, true)
            result shouldBe "Pass to Main 2"
        }

        test("at DECLARE_BLOCKERS with no stateProjector defaults to 'Resolve combat damage'") {
            val attacker = EntityId.generate()
            val (state, _) = createCombatState(
                step = Step.DECLARE_BLOCKERS,
                attackerIds = listOf(attacker),
                firstStrikeEntityIds = setOf(attacker) // Has first strike but no projector
            )
            // Without stateProjector, hasCombatFirstStrike returns false
            autoPassManager.getNextStopPoint(state, player1, true, null) shouldBe "Resolve combat damage"
        }
    }
})
