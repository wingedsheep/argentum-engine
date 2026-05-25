package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Helpers for calculating the amount of combat damage a creature assigns.
 *
 * Most creatures assign damage equal to their power, but some cards
 * (Doran the Siege Tower, Bark of Doran) substitute toughness — either
 * always or only when toughness exceeds power.
 */
internal object CombatDamageUtils {

    private val predicateEvaluator = PredicateEvaluator()

    /** Which side of combat a damage source is on, for [combatDamageChooser]. */
    enum class CombatSide { ATTACKER, BLOCKER }

    /**
     * Who divides a source's combat damage, and whether CR 510.1c assignment order applies.
     *
     * @property playerId The player who chooses this source's damage division.
     * @property orderConstrained False only when banding (CR 702.22j/k) lets the chooser ignore
     *   the damage-assignment order. Note this is the *only* thing banding relaxes about order:
     *   a banded attacker's own [ATTACKER][CombatSide.ATTACKER]-side edges stay order-constrained
     *   unless one of its blockers has banding — band cooperation falls out of CR 510.1c's
     *   cross-source lethal counting in the validator, not from lifting the attacker's order.
     */
    data class DamageChooser(val playerId: EntityId, val orderConstrained: Boolean)

    /**
     * Resolve who assigns [sourceId]'s combat damage and whether its assignment order is
     * constrained, applying the banding inversions (CR 702.22j/k) on top of the normal
     * CR 510.1c defaults. This single entry point replaces the older split of
     * `damageAssignmentChooser` / `blockerDamageAssignmentChooser` / `attackerBandHasBanding`.
     *
     * - [ATTACKER][CombatSide.ATTACKER]: default is the attacking player. If any creature
     *   blocking [sourceId] has banding, CR 702.22j hands the division to the defending player
     *   and lifts the order constraint.
     * - [BLOCKER][CombatSide.BLOCKER]: default is the blocker's controller ([defaultChooser]).
     *   If any attacker [sourceId] is blocking has banding, CR 702.22k hands the division to the
     *   active player and lifts the order constraint.
     */
    fun combatDamageChooser(
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId,
        side: CombatSide,
        defaultChooser: EntityId,
        activePlayerId: EntityId,
    ): DamageChooser = when (side) {
        CombatSide.ATTACKER -> attackerDamageChooser(state, projected, sourceId, defaultChooser)
        CombatSide.BLOCKER -> blockerDamageChooser(state, projected, sourceId, defaultChooser, activePlayerId)
    }

    private fun attackerDamageChooser(
        state: GameState,
        projected: ProjectedState,
        attackerId: EntityId,
        defaultChooser: EntityId,
    ): DamageChooser {
        val container = state.getEntity(attackerId) ?: return DamageChooser(defaultChooser, orderConstrained = true)
        val blockedBy = container.get<BlockedComponent>()
        val blockerHasBanding = blockedBy?.blockerIds?.any { projected.hasKeyword(it, Keyword.BANDING) } == true
        if (!blockerHasBanding) return DamageChooser(defaultChooser, orderConstrained = true)

        // CR 702.22j: the defending player divides this attacker's damage "as they choose".
        val defenderId = container.get<AttackingComponent>()?.defenderId
        val chooser = when {
            defenderId == null -> defaultChooser
            state.turnOrder.contains(defenderId) -> defenderId
            else -> projected.getController(defenderId) ?: defaultChooser
        }
        return DamageChooser(chooser, orderConstrained = false)
    }

    private fun blockerDamageChooser(
        state: GameState,
        projected: ProjectedState,
        blockerId: EntityId,
        defaultChooser: EntityId,
        activePlayerId: EntityId,
    ): DamageChooser {
        val blocking = state.getEntity(blockerId)?.get<BlockingComponent>()
            ?: return DamageChooser(defaultChooser, orderConstrained = true)
        // CR 702.22k: if any blocked attacker has banding, the active player divides this
        // blocker's damage "as they choose".
        val attackerHasBanding = blocking.blockedAttackerIds.any { projected.hasKeyword(it, Keyword.BANDING) }
        return if (attackerHasBanding) {
            DamageChooser(activePlayerId, orderConstrained = false)
        } else {
            DamageChooser(defaultChooser, orderConstrained = true)
        }
    }

    /**
     * Returns the combat damage amount that [creatureId] assigns this step.
     *
     * Defaults to the creature's projected power. If the creature (or any
     * permanent attached to it) has [AssignDamageEqualToToughness] and that
     * ability's condition holds, returns projected toughness instead.
     */
    fun getAssignedCombatDamage(
        state: GameState,
        projected: ProjectedState,
        creatureId: EntityId,
        cardRegistry: CardRegistry?,
    ): Int {
        val power = projected.getPower(creatureId) ?: 0
        if (cardRegistry == null) return power

        val toughness = projected.getToughness(creatureId) ?: 0
        return if (assignsDamageAsToughness(state, projected, creatureId, cardRegistry, power, toughness)) {
            toughness.coerceAtLeast(0)
        } else {
            power
        }
    }

    private fun assignsDamageAsToughness(
        state: GameState,
        projected: ProjectedState,
        creatureId: EntityId,
        cardRegistry: CardRegistry,
        power: Int,
        toughness: Int,
    ): Boolean {
        // The creature itself (e.g., Doran the Siege Tower, filter scope = Self)
        val selfCardId = state.getEntity(creatureId)?.get<CardComponent>()?.cardDefinitionId
        if (selfCardId != null) {
            val abilities = cardRegistry.getCard(selfCardId)?.staticAbilities.orEmpty()
            if (matches(abilities, Scope.Self, power, toughness)) return true
        }

        // Equipment/Aura attached to the creature (e.g., Bark of Doran, filter scope = AttachedTo)
        val attachments = state.getEntity(creatureId)?.get<AttachmentsComponent>()?.attachedIds.orEmpty()
        for (attachId in attachments) {
            val attachCardId = state.getEntity(attachId)?.get<CardComponent>()?.cardDefinitionId ?: continue
            val abilities = cardRegistry.getCard(attachCardId)?.staticAbilities.orEmpty()
            if (matches(abilities, Scope.AttachedTo, power, toughness)) return true
        }

        // Global permanents with Scope.Battlefield (e.g., Tapestry Warden: "creatures you control")
        // The source may equal the creature (e.g., Tapestry Warden applying to itself).
        for (permanentId in state.getBattlefield()) {
            val permCardId = state.getEntity(permanentId)?.get<CardComponent>()?.cardDefinitionId ?: continue
            val abilities = cardRegistry.getCard(permCardId)?.staticAbilities.orEmpty()
            if (matchesBattlefield(state, projected, permanentId, creatureId, abilities, power, toughness)) return true
        }

        return false
    }

    private fun matchesBattlefield(
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId,
        creatureId: EntityId,
        abilities: List<StaticAbility>,
        power: Int,
        toughness: Int,
    ): Boolean {
        val sourceController = projected.getController(sourceId) ?: return false
        val predicateContext = PredicateContext(controllerId = sourceController, sourceId = sourceId)
        for (ability in abilities) {
            val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
            if (unwrapped !is AssignDamageEqualToToughness) continue
            if (unwrapped.filter.scope !is Scope.Battlefield) continue
            // Honor excludeSelf: skip if this ability excludes the source and creature is the source
            if (unwrapped.filter.excludeSelf && sourceId == creatureId) continue
            if (unwrapped.onlyWhenToughnessGreaterThanPower && toughness <= power) continue
            // Defer the full filter check (controller, type, subtype, keywords, state) to the
            // shared PredicateEvaluator so new battlefield-scope uses (e.g. "Each Equipment-
            // bearing creature you control") are honored without per-call special-casing.
            if (!predicateEvaluator.matches(state, projected, creatureId, unwrapped.filter.baseFilter, predicateContext)) continue
            return true
        }
        return false
    }

    private fun matches(
        abilities: List<StaticAbility>,
        expectedScope: Scope,
        power: Int,
        toughness: Int,
    ): Boolean {
        for (ability in abilities) {
            val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
            if (unwrapped !is AssignDamageEqualToToughness) continue
            if (unwrapped.filter.scope != expectedScope) continue
            if (unwrapped.onlyWhenToughnessGreaterThanPower && toughness <= power) continue
            return true
        }
        return false
    }
}
