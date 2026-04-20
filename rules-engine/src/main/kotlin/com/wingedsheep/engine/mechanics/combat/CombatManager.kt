package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.mechanics.combat.rules.AttackDefenderRule
import com.wingedsheep.engine.mechanics.combat.rules.AttackRestrictionRule
import com.wingedsheep.engine.mechanics.combat.rules.BlockEvasionRule
import com.wingedsheep.engine.mechanics.combat.rules.defaultAttackDefenderRules
import com.wingedsheep.engine.mechanics.combat.rules.defaultAttackRestrictionRules
import com.wingedsheep.engine.mechanics.combat.rules.defaultBlockEvasionRules

/**
 * Manages combat flow: attackers, blockers, damage.
 *
 * This is a thin facade that delegates to phase-specific managers:
 * - [AttackPhaseManager] — declare attackers, attack validation, attack taxes
 * - [BlockPhaseManager] — declare blockers, block validation, evasion, block taxes
 * - [CombatDamageManager] — combat damage calculation and application
 *
 * Combat proceeds through these steps:
 * 1. Beginning of combat step
 * 2. Declare attackers step
 * 3. Declare blockers step
 * 4. Combat damage step (first strike, then regular)
 * 5. End of combat step
 */
class CombatManager(
    private val cardRegistry: CardRegistry,
    private val damageCalculator: DamageCalculator = DamageCalculator(cardRegistry),
    private val blockEvasionRules: List<BlockEvasionRule> = defaultBlockEvasionRules(),
    private val attackRestrictionRules: List<AttackRestrictionRule> = defaultAttackRestrictionRules(),
    private val attackDefenderRules: List<AttackDefenderRule> = defaultAttackDefenderRules(),
) {
    private val attackPhase = AttackPhaseManager(cardRegistry, attackRestrictionRules, attackDefenderRules)
    private val blockPhase = BlockPhaseManager(cardRegistry, blockEvasionRules)
    private val damagePhase = CombatDamageManager(cardRegistry, damageCalculator)

    // =========================================================================
    // Declare Attackers
    // =========================================================================

    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): ExecutionResult = attackPhase.declareAttackers(state, attackingPlayer, attackers)

    fun isValidAttacker(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean =
        attackPhase.isValidAttacker(state, attackerId, attackingPlayer)

    fun isRestrictedFromAllDefenders(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean =
        attackPhase.isRestrictedFromAllDefenders(state, attackerId, attackingPlayer)

    // =========================================================================
    // Declare Blockers
    // =========================================================================

    fun declareBlockers(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): ExecutionResult = blockPhase.declareBlockers(state, blockingPlayer, blockers)

    fun canCreatureBlockAnyAttacker(state: GameState, blockerId: EntityId, blockingPlayer: EntityId): Boolean =
        blockPhase.canCreatureBlockAnyAttacker(state, blockerId, blockingPlayer)

    fun getMandatoryBlockerAssignments(state: GameState, blockingPlayer: EntityId): Map<EntityId, List<EntityId>> =
        blockPhase.getMandatoryBlockerAssignments(state, blockingPlayer)

    fun getMandatoryAttackers(state: GameState, attackingPlayer: EntityId): List<EntityId> =
        attackPhase.getMandatoryAttackers(state, attackingPlayer)

    fun createAttackerOrderDecision(
        state: GameState,
        attackingPlayer: EntityId,
        firstBlocker: EntityId,
        remainingBlockers: List<EntityId>,
        precedingEvents: List<GameEvent>
    ): ExecutionResult = blockPhase.createAttackerOrderDecision(
        state, attackingPlayer, firstBlocker, remainingBlockers, precedingEvents
    )

    // =========================================================================
    // Combat Damage
    // =========================================================================

    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult =
        damagePhase.applyCombatDamage(state, firstStrike)

    // =========================================================================
    // End Combat
    // =========================================================================

    fun endCombat(state: GameState): ExecutionResult {
        var newState = state

        for ((entityId, _) in state.entities) {
            newState = newState.updateEntity(entityId) { container ->
                container
                    .without<AttackingComponent>()
                    .without<BlockingComponent>()
                    .without<BlockedComponent>()
                    .without<DamageAssignmentComponent>()
                    .without<DamageAssignmentOrderComponent>()
                    .without<AttackerOrderComponent>()
                    .without<DealtFirstStrikeDamageComponent>()
                    .without<RequiresManualDamageAssignmentComponent>()
                    .without<AttackersDeclaredThisCombatComponent>()
                    .without<BlockersDeclaredThisCombatComponent>()
            }
        }

        return ExecutionResult.success(newState)
    }

    // =========================================================================
    // Queries
    // =========================================================================

    fun getAttackers(state: GameState): List<EntityId> =
        state.findEntitiesWith<AttackingComponent>().map { it.first }

    fun getBlockers(state: GameState): List<EntityId> =
        state.findEntitiesWith<BlockingComponent>().map { it.first }

    fun hasAttackers(state: GameState): Boolean =
        state.findEntitiesWith<AttackingComponent>().isNotEmpty()
}
