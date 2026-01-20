package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition

/**
 * Registry that maps card definitions to their abilities.
 * This serves as the central repository for card scripting.
 */
class AbilityRegistry {
    private val triggeredAbilities = mutableMapOf<String, List<TriggeredAbility>>()
    private val activatedAbilities = mutableMapOf<String, List<ActivatedAbility>>()
    private val staticAbilities = mutableMapOf<String, List<StaticAbility>>()

    /**
     * Register triggered abilities for a card (by name).
     */
    fun registerTriggeredAbilities(cardName: String, abilities: List<TriggeredAbility>) {
        triggeredAbilities[cardName] = abilities
    }

    /**
     * Register a single triggered ability for a card.
     */
    fun registerTriggeredAbility(cardName: String, ability: TriggeredAbility) {
        val existing = triggeredAbilities[cardName] ?: emptyList()
        triggeredAbilities[cardName] = existing + ability
    }

    /**
     * Get triggered abilities for a card definition.
     */
    fun getTriggeredAbilities(definition: CardDefinition): List<TriggeredAbility> {
        return triggeredAbilities[definition.name] ?: emptyList()
    }

    /**
     * Register activated abilities for a card.
     */
    fun registerActivatedAbilities(cardName: String, abilities: List<ActivatedAbility>) {
        activatedAbilities[cardName] = abilities
    }

    /**
     * Get activated abilities for a card definition.
     */
    fun getActivatedAbilities(definition: CardDefinition): List<ActivatedAbility> {
        return activatedAbilities[definition.name] ?: emptyList()
    }

    /**
     * Register static abilities for a card.
     */
    fun registerStaticAbilities(cardName: String, abilities: List<StaticAbility>) {
        staticAbilities[cardName] = abilities
    }

    /**
     * Register a single static ability for a card.
     */
    fun registerStaticAbility(cardName: String, ability: StaticAbility) {
        val existing = staticAbilities[cardName] ?: emptyList()
        staticAbilities[cardName] = existing + ability
    }

    /**
     * Get static abilities for a card definition.
     */
    fun getStaticAbilities(definition: CardDefinition): List<StaticAbility> {
        return staticAbilities[definition.name] ?: emptyList()
    }

    /**
     * Check if a card has any abilities registered.
     */
    fun hasAbilities(cardName: String): Boolean {
        return triggeredAbilities.containsKey(cardName) ||
                activatedAbilities.containsKey(cardName) ||
                staticAbilities.containsKey(cardName)
    }

    /**
     * Clear all registered abilities.
     */
    fun clear() {
        triggeredAbilities.clear()
        activatedAbilities.clear()
        staticAbilities.clear()
    }

    companion object {
        /**
         * Create a registry with abilities for common Portal cards.
         */
        fun createPortalRegistry(): AbilityRegistry {
            val registry = AbilityRegistry()
            // Abilities would be registered here for Portal cards
            // This will be populated in Phase 13 (Portal Set Implementation)
            return registry
        }
    }
}

/**
 * Activated abilities (tap abilities, mana abilities, cycling, etc.)
 *
 * @property id Unique identifier for this ability
 * @property cost The cost to activate (tap, mana, sacrifice, etc.)
 * @property effect The effect that occurs when activated
 * @property timingRestriction When this ability can be activated
 * @property isManaAbility If true, this ability adds mana and doesn't use the stack.
 *                         Mana abilities resolve immediately and can be activated
 *                         while paying costs (Rule 605).
 * @property activateFromZone The zone this ability can be activated from.
 *                            Most abilities are from BATTLEFIELD, cycling is from HAND.
 */
@kotlinx.serialization.Serializable
data class ActivatedAbility(
    val id: AbilityId,
    val cost: AbilityCost,
    val effect: Effect,
    val timingRestriction: TimingRestriction = TimingRestriction.INSTANT,
    val isManaAbility: Boolean = false,
    val isPlaneswalkerAbility: Boolean = false,
    val activateFromZone: com.wingedsheep.rulesengine.zone.ZoneType = com.wingedsheep.rulesengine.zone.ZoneType.BATTLEFIELD
)

/**
 * Cost to activate an ability.
 */
@kotlinx.serialization.Serializable
sealed interface AbilityCost {
    @kotlinx.serialization.Serializable
    data object Tap : AbilityCost
    @kotlinx.serialization.Serializable
    data class Mana(val white: Int = 0, val blue: Int = 0, val black: Int = 0, val red: Int = 0, val green: Int = 0, val generic: Int = 0) : AbilityCost
    @kotlinx.serialization.Serializable
    data class Composite(val costs: List<AbilityCost>) : AbilityCost
    @kotlinx.serialization.Serializable
    data class PayLife(val amount: Int) : AbilityCost
    @kotlinx.serialization.Serializable
    data class Sacrifice(val filter: String) : AbilityCost
    @kotlinx.serialization.Serializable
    data class Discard(val count: Int) : AbilityCost
    @kotlinx.serialization.Serializable
    data class RemoveCounter(val counterType: String, val count: Int = 1) : AbilityCost
    @kotlinx.serialization.Serializable
    data class Loyalty(val amount: Int) : AbilityCost  // Positive for +X, negative for -X
    /**
     * Tap another untapped creature you control as a cost.
     * @param count Number of creatures to tap
     * @param filter Optional filter for valid targets (e.g., "untapped Merfolk you control")
     * @param targetId The entity ID of the creature chosen to tap (filled in during cost payment)
     */
    @kotlinx.serialization.Serializable
    data class TapOtherCreature(
        val count: Int = 1,
        val filter: String = "untapped creature you control",
        val targetId: com.wingedsheep.rulesengine.ecs.EntityId? = null
    ) : AbilityCost

    /**
     * Blight N: Put N -1/-1 counters on a creature you control as a cost.
     * This is a Lorwyn Eclipsed mechanic that represents decay/sacrifice.
     *
     * @param amount Number of -1/-1 counters to place
     * @param targetId The entity ID of the creature to blight (filled in during cost payment)
     */
    @kotlinx.serialization.Serializable
    data class Blight(
        val amount: Int,
        val targetId: com.wingedsheep.rulesengine.ecs.EntityId? = null
    ) : AbilityCost

    /**
     * Discard this card as a cost.
     * Used for cycling and similar abilities activated from hand.
     */
    @kotlinx.serialization.Serializable
    data object DiscardSelf : AbilityCost
}

/**
 * Timing restriction for activated abilities.
 */
@kotlinx.serialization.Serializable
enum class TimingRestriction {
    INSTANT,    // Can activate any time you have priority
    SORCERY,    // Only during main phase, empty stack
    ATTACK,     // Only during declare attackers
    BLOCK       // Only during declare blockers
}

// StaticAbility is defined in StaticAbility.kt as a sealed interface
