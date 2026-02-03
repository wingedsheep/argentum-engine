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
        hasPendingDecision: Boolean = false
    ): GameState {
        val state = mockk<GameState>(relaxed = true)
        every { state.priorityPlayerId } returns priorityPlayerId
        every { state.activePlayerId } returns activePlayerId
        every { state.step } returns step
        every { state.phase } returns step.phase
        every { state.stack } returns if (stackEmpty) emptyList() else listOf(EntityId.generate())
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

    context("Rule 4: Stack Response - Absolute Rule") {
        test("STOP when stack is non-empty and have responses") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false)
            val actions = listOf(
                passPriorityAction(player1),
                instantSpellAction(player1)
            )

            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
        }

        test("STOP when stack is non-empty even during opponent's main") {
            val state = createMockState(player2, player1, Step.PRECOMBAT_MAIN, stackEmpty = false)
            val actions = listOf(
                passPriorityAction(player2),
                instantSpellAction(player2)
            )

            autoPassManager.shouldAutoPass(state, player2, actions) shouldBe false
        }

        test("STOP when stack is non-empty even with no responses available") {
            val state = createMockState(player1, player1, Step.PRECOMBAT_MAIN, stackEmpty = false)
            val actions = listOf(
                passPriorityAction(player1),
                manaAbilityAction(player1) // Only mana ability, not a response
            )

            // Always stop when stack is non-empty so player can see what was cast
            autoPassManager.shouldAutoPass(state, player1, actions) shouldBe false
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
