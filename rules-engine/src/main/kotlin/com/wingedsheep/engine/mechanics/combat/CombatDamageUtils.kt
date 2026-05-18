package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
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

    /**
     * Who chooses the damage-assignment order / division for [attackerId]'s combat damage
     * among its blockers?
     *
     * Default: the attacker's controller (CR 510.1c). Inverted to the defending player when
     * any defending creature blocking [attackerId] has banding (CR 702.21e). When no
     * inversion applies, returns [defaultChooser].
     *
     * The "defender" is the controller of `AttackingComponent.defenderId` — either the
     * defending player directly or the controller of the attacked planeswalker.
     */
    fun damageAssignmentChooser(
        state: GameState,
        projected: ProjectedState,
        attackerId: EntityId,
        defaultChooser: EntityId,
    ): EntityId {
        val attackerContainer = state.getEntity(attackerId) ?: return defaultChooser
        val blockedBy = attackerContainer.get<BlockedComponent>() ?: return defaultChooser
        val blockerHasBanding = blockedBy.blockerIds.any { blockerId ->
            projected.hasKeyword(blockerId, Keyword.BANDING)
        }
        if (!blockerHasBanding) return defaultChooser

        val attacking = attackerContainer.get<AttackingComponent>() ?: return defaultChooser
        val defenderId = attacking.defenderId
        return if (state.turnOrder.contains(defenderId)) defenderId
            else projected.getController(defenderId) ?: defaultChooser
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
