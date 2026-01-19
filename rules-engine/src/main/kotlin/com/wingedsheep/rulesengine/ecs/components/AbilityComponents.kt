package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ability.ActivatedAbility
import com.wingedsheep.rulesengine.ability.StaticAbility
import com.wingedsheep.rulesengine.ability.TriggeredAbility
import com.wingedsheep.rulesengine.ecs.Component
import kotlinx.serialization.Serializable

/**
 * Stores abilities that have been "baked" onto an entity.
 *
 * In the ECS model, abilities are stored directly on entities rather than
 * being looked up from a registry each time. This allows:
 * - Faster ability lookups during gameplay
 * - Abilities to be modified by effects (gaining/losing abilities)
 * - Copy effects to include abilities
 *
 * @property activatedAbilities All activated abilities on this entity
 * @property triggeredAbilities All triggered abilities on this entity
 * @property staticAbilities All static abilities on this entity
 */
@Serializable
data class AbilitiesComponent(
    val activatedAbilities: List<ActivatedAbility> = emptyList(),
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    val staticAbilities: List<StaticAbility> = emptyList()
) : Component {

    /**
     * True if this entity has any mana abilities.
     * Mana abilities are activated abilities that add mana and don't use the stack.
     */
    val hasManaAbilities: Boolean
        get() = activatedAbilities.any { it.isManaAbility }

    /**
     * Get all mana abilities on this entity.
     */
    val manaAbilities: List<ActivatedAbility>
        get() = activatedAbilities.filter { it.isManaAbility }

    /**
     * Get all non-mana activated abilities on this entity.
     */
    val nonManaActivatedAbilities: List<ActivatedAbility>
        get() = activatedAbilities.filter { !it.isManaAbility }

    /**
     * Add an activated ability to this component.
     */
    fun addActivatedAbility(ability: ActivatedAbility): AbilitiesComponent =
        copy(activatedAbilities = activatedAbilities + ability)

    /**
     * Add a triggered ability to this component.
     */
    fun addTriggeredAbility(ability: TriggeredAbility): AbilitiesComponent =
        copy(triggeredAbilities = triggeredAbilities + ability)

    /**
     * Add a static ability to this component.
     */
    fun addStaticAbility(ability: StaticAbility): AbilitiesComponent =
        copy(staticAbilities = staticAbilities + ability)

    /**
     * Remove an activated ability from this component.
     */
    fun removeActivatedAbility(abilityId: String): AbilitiesComponent =
        copy(activatedAbilities = activatedAbilities.filter { it.id.value != abilityId })

    /**
     * Get an activated ability by index.
     */
    fun getActivatedAbility(index: Int): ActivatedAbility? =
        activatedAbilities.getOrNull(index)

    /**
     * Get a mana ability by index (within mana abilities list).
     */
    fun getManaAbility(index: Int): ActivatedAbility? =
        manaAbilities.getOrNull(index)

    companion object {
        val EMPTY = AbilitiesComponent()
    }
}
