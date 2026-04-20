package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Helpers for calculating the amount of combat damage a creature assigns.
 *
 * Most creatures assign damage equal to their power, but some cards
 * (Doran the Siege Tower, Bark of Doran) substitute toughness — either
 * always or only when toughness exceeds power.
 */
internal object CombatDamageUtils {

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
        return if (assignsDamageAsToughness(state, creatureId, cardRegistry, power, toughness)) {
            toughness.coerceAtLeast(0)
        } else {
            power
        }
    }

    private fun assignsDamageAsToughness(
        state: GameState,
        creatureId: EntityId,
        cardRegistry: CardRegistry,
        power: Int,
        toughness: Int,
    ): Boolean {
        // The creature itself (e.g., Doran the Siege Tower, target = SourceCreature)
        val selfCardId = state.getEntity(creatureId)?.get<CardComponent>()?.cardDefinitionId
        if (selfCardId != null) {
            val abilities = cardRegistry.getCard(selfCardId)?.staticAbilities.orEmpty()
            if (matches(abilities, StaticTarget.SourceCreature, power, toughness)) return true
        }

        // Equipment/Aura attached to the creature (e.g., Bark of Doran, target = AttachedCreature)
        val attachments = state.getEntity(creatureId)?.get<AttachmentsComponent>()?.attachedIds.orEmpty()
        for (attachId in attachments) {
            val attachCardId = state.getEntity(attachId)?.get<CardComponent>()?.cardDefinitionId ?: continue
            val abilities = cardRegistry.getCard(attachCardId)?.staticAbilities.orEmpty()
            if (matches(abilities, StaticTarget.AttachedCreature, power, toughness)) return true
        }

        return false
    }

    private fun matches(
        abilities: List<StaticAbility>,
        expectedTarget: StaticTarget,
        power: Int,
        toughness: Int,
    ): Boolean {
        for (ability in abilities) {
            val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
            if (unwrapped !is AssignDamageEqualToToughness) continue
            if (unwrapped.target != expectedTarget) continue
            if (unwrapped.onlyWhenToughnessGreaterThanPower && toughness <= power) continue
            return true
        }
        return false
    }
}
