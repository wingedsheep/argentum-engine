package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

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
        stackControllerId: EntityId? = null
    ): GameState {
        val state = mockk<GameState>(relaxed = true)
        every { state.priorityPlayerId } returns priorityPlayerId
        every { state.activePlayerId } returns activePlayerId
        every { state.step } returns step
        every { state.phase } returns step.phase

        if (stackEmpty) {
            every { state.stack } returns emptyList()
        } else {
            val stackItemId = EntityId.generate()
            every { state.stack } returns listOf(stackItemId)

            // Mock the stack item entity with appropriate component
            val stackEntity = mockk<com.wingedsheep.engine.state.ComponentContainer>(relaxed = true)
            every { state.getEntity(stackItemId) } returns stackEntity

            // Set up the controller - use ActivatedAbilityOnStackComponent for abilities
            val controllerId = stackControllerId ?: activePlayerId
            val abilityComponent = com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent(
                sourceId = EntityId.generate(),
                sourceName = "Test Ability",
                controllerId = controllerId,
                effect = mockk(relaxed = true)
            )
            every { stackEntity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>() } returns abilityComponent
            every { stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>() } returns null
            every { stackEntity.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>() } returns null
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

    context("Rule 2: Opponent's Turn Compression") {
        test("Auto-pass during opponent's upkeep") {
            val state = createMockState(player2, player1, Step.UPKEEP) // player2's turn
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's draw step") {
            val state = createMockState(player2, player1, Step.DRAW)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's main phase") {
            val state = createMockState(player2, player1, Step.PRECOMBAT_MAIN)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("STOP during opponent's begin combat") {
            val state = createMockState(player2, player1, Step.BEGIN_COMBAT)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("STOP during opponent's declare attackers") {
            val state = createMockState(player2, player1, Step.DECLARE_ATTACKERS)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("STOP during opponent's end step - the golden rule") {
            val state = createMockState(player2, player1, Step.END)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("Auto-pass during opponent's begin combat when no meaningful actions") {
            val state = createMockState(player2, player1, Step.BEGIN_COMBAT)
            val actions = listOf(
                passPriorityAction(player2),
                manaAbilityAction(player2) // Only mana ability, not meaningful
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during opponent's declare attackers when no meaningful actions") {
            val state = createMockState(player2, player1, Step.DECLARE_ATTACKERS)
            val actions = listOf(
                passPriorityAction(player2) // Only pass priority available
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("Auto-pass during declare blockers when no blockers or responses available") {
            val state = createMockState(player2, player1, Step.DECLARE_BLOCKERS)
            val actions = listOf(
                passPriorityAction(player2) // No blockers, no instants
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe true
        }

        test("STOP during declare blockers when blockers or responses available") {
            val state = createMockState(player2, player1, Step.DECLARE_BLOCKERS)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2) // Has a response available
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }
    }

    context("Rule 3: My Turn Optimization") {
        test("Auto-pass during my upkeep") {
            val state = createMockState(player1, player1, Step.UPKEEP)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe true
        }

        test("Auto-pass during my draw step") {
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

        test("STOP during my declare blockers (for combat tricks)") {
            val state = createMockState(player1, player1, Step.DECLARE_BLOCKERS)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
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
})
