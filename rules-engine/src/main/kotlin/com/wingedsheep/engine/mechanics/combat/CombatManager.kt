package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
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
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val damageCalculator: DamageCalculator = DamageCalculator(cardRegistry),
    private val blockEvasionRules: List<BlockEvasionRule> = defaultBlockEvasionRules(),
    private val attackRestrictionRules: List<AttackRestrictionRule> = defaultAttackRestrictionRules(),
    private val attackDefenderRules: List<AttackDefenderRule> = defaultAttackDefenderRules(),
) {
    internal val attackPhase = AttackPhaseManager(cardRegistry, attackRestrictionRules, attackDefenderRules, manaAbilitySideEffectExecutor)
    internal val blockPhase = BlockPhaseManager(cardRegistry, blockEvasionRules, manaAbilitySideEffectExecutor)
    private val damagePhase = CombatDamageManager(cardRegistry, damageCalculator)

    // =========================================================================
    // Declare Attackers
    // =========================================================================

    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        bands: List<Set<EntityId>> = emptyList()
    ): ExecutionResult = attackPhase.declareAttackers(state, attackingPlayer, attackers, bands)

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

    // =========================================================================
    // Combat Damage
    // =========================================================================

    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult =
        damagePhase.applyCombatDamage(state, firstStrike)

    /**
     * Drop the damage assignments chosen in the first-strike combat damage step, so the regular
     * one assigns from scratch.
     *
     * CR 510.1 / 510.4: the second combat damage step is its own assignment — every creature that
     * still deals damage (double strikers, plus everything that had no first strike) announces a
     * fresh division. Carrying the first-strike [DamageAssignmentComponent] over is wrong whenever
     * first-strike damage killed only *some* of the blockers: the amounts aimed at the dead ones
     * would spill onto the defending player through trample, while the blockers still blocking
     * were never assigned the lethal damage CR 702.19b requires first.
     *
     * Only the assignment is cleared. Damage assignment *order* ([DamageAssignmentOrderComponent],
     * [AttackerOrderComponent]) is chosen once per combat and stays put.
     */
    fun clearDamageAssignmentsForNewDamageStep(state: GameState): GameState {
        var newState = state
        for ((entityId, _) in state.findEntitiesWith<DamageAssignmentComponent>()) {
            newState = newState.updateEntity(entityId) { container ->
                container.without<DamageAssignmentComponent>()
            }
        }
        return newState
    }

    /**
     * Re-pause the same combat damage step for the next chooser (CR 510.1c sequencing and the
     * CR 702.22j/k two-actor banding case). Delegates to [CombatDamageManager.repauseCombatResolution].
     */
    fun repauseCombatResolution(
        state: GameState,
        previous: com.wingedsheep.engine.core.CombatResolutionDecision,
        remainingChoosers: List<EntityId>,
        latestAmounts: Map<String, Int>,
        firstStrike: Boolean,
    ): ExecutionResult = damagePhase.repauseCombatResolution(
        state, previous, remainingChoosers, latestAmounts, firstStrike,
    )

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
                    .without<AttackedThisCombatComponent>()
                    .without<BlockedThisCombatComponent>()
            }
        }

        // Discard combat-duration mana (firebending, CR 702.189): "Any of this mana you still
        // have as combat ends will be lost." Ordinary (end-of-turn) mana is untouched. A player
        // controlling a ConvertEmptyingManaToRed permanent (Ozai, the Phoenix King) instead has
        // that would-be-lost mana become red (CR 614) — it survives combat as ordinary red mana,
        // exactly as the end-of-turn cleanup path already handles the general pool.
        val convertToRedPlayers = playersConvertingEmptyingManaToRed(newState, cardRegistry)
        for (playerId in newState.turnOrder) {
            val pool = newState.getEntity(playerId)?.get<ManaPoolComponent>() ?: continue
            if (pool.restrictedMana.any { it.expiry == ManaExpiry.END_OF_COMBAT }) {
                newState = newState.updateEntity(playerId) { container ->
                    if (playerId in convertToRedPlayers) {
                        container.with(pool.convertExpiredToRed(ManaExpiry.END_OF_COMBAT))
                    } else {
                        container.with(pool.clearExpired(ManaExpiry.END_OF_COMBAT))
                    }
                }
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
