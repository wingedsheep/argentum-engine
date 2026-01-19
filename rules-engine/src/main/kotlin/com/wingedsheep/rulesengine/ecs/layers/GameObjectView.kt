package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId

/**
 * A projected view of a game object after all continuous effects are applied.
 *
 * This represents the "current" state of an entity as seen by the game rules,
 * after all modifiers from Layer 1-7 have been applied. The base entity data
 * in the GameState remains unchanged; this is a calculated view.
 *
 * Example:
 * - Base state: Grizzly Bears (2/2, no abilities)
 * - Equipped with Loxodon Warhammer (+3/+0, trample, lifelink)
 * - Affected by Glorious Anthem (+1/+1 for your creatures)
 * - GameObjectView shows: 6/3 with trample and lifelink
 *
 * @property entityId The entity this view represents
 * @property controllerId The current controller (after Layer 2)
 * @property types Current card types (after Layer 4)
 * @property subtypes Current subtypes (after Layer 4)
 * @property colors Current colors (after Layer 5)
 * @property keywords Current keywords (after Layer 6)
 * @property power Current power (after Layer 7), null for non-creatures
 * @property toughness Current toughness (after Layer 7), null for non-creatures
 * @property hasAbilities Whether the object still has its printed abilities (false if removed by Humility, etc.)
 * @property counters All counters on this permanent (empty map for none)
 * @property attachedTo The entity this is attached to (for auras/equipment), null if not attached
 * @property attachments Entities attached to this permanent (reverse lookup for convenience)
 * @property loyalty Current loyalty for planeswalkers (derived from loyalty counters), null for non-planeswalkers
 * @property cantBlock Whether this creature is prevented from blocking (defaults to false)
 */
data class GameObjectView(
    val entityId: EntityId,
    val name: String,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val types: Set<CardType>,
    val subtypes: Set<Subtype>,
    val colors: Set<Color>,
    val keywords: Set<Keyword>,
    val power: Int?,
    val toughness: Int?,
    val hasAbilities: Boolean = true,
    val damage: Int = 0,
    val isTapped: Boolean = false,
    val hasSummoningSickness: Boolean = false,
    val counters: Map<CounterType, Int> = emptyMap(),
    val attachedTo: EntityId? = null,
    val attachments: List<EntityId> = emptyList(),
    val loyalty: Int? = null,
    val cantBlock: Boolean = false
) {
    /**
     * Check if this object is a creature.
     */
    val isCreature: Boolean
        get() = CardType.CREATURE in types

    /**
     * Check if this object is a land.
     */
    val isLand: Boolean
        get() = CardType.LAND in types

    /**
     * Check if this object is an artifact.
     */
    val isArtifact: Boolean
        get() = CardType.ARTIFACT in types

    /**
     * Check if this object is an enchantment.
     */
    val isEnchantment: Boolean
        get() = CardType.ENCHANTMENT in types

    /**
     * Check if this object is an instant.
     */
    val isInstant: Boolean
        get() = CardType.INSTANT in types

    /**
     * Check if this object is a sorcery.
     */
    val isSorcery: Boolean
        get() = CardType.SORCERY in types

    /**
     * Check if this object is a planeswalker.
     */
    val isPlaneswalker: Boolean
        get() = CardType.PLANESWALKER in types

    /**
     * Check if this is a permanent type.
     */
    val isPermanent: Boolean
        get() = isCreature || isLand || isArtifact || isEnchantment || isPlaneswalker

    /**
     * Check if this creature has a specific keyword.
     */
    fun hasKeyword(keyword: Keyword): Boolean = keyword in keywords

    /**
     * Calculate effective toughness (toughness minus damage).
     */
    val effectiveToughness: Int?
        get() = toughness?.let { it - damage }

    /**
     * Check if this creature has lethal damage marked.
     */
    val hasLethalDamage: Boolean
        get() = effectiveToughness?.let { it <= 0 } ?: false

    /**
     * Check if this creature can attack (not tapped, no summoning sickness or has haste).
     */
    val canAttack: Boolean
        get() = isCreature &&
                !isTapped &&
                (!hasSummoningSickness || hasKeyword(Keyword.HASTE)) &&
                !hasKeyword(Keyword.DEFENDER)

    /**
     * Check if this creature can block.
     */
    val canBlock: Boolean
        get() = isCreature && !isTapped && !cantBlock

    /**
     * Check if this creature has flying or reach (for blocking purposes).
     */
    val canBlockFlying: Boolean
        get() = hasKeyword(Keyword.FLYING) || hasKeyword(Keyword.REACH)

    // ==========================================================================
    // Counter Accessors
    // ==========================================================================

    /**
     * Check if this permanent has any counters of the specified type.
     */
    fun hasCounter(type: CounterType): Boolean = (counters[type] ?: 0) > 0

    /**
     * Get the count of a specific counter type.
     */
    fun getCounterCount(type: CounterType): Int = counters[type] ?: 0

    /**
     * Get total +1/+1 counter count.
     */
    val plusOnePlusOneCounters: Int
        get() = getCounterCount(CounterType.PLUS_ONE_PLUS_ONE)

    /**
     * Get total -1/-1 counter count.
     */
    val minusOneMinusOneCounters: Int
        get() = getCounterCount(CounterType.MINUS_ONE_MINUS_ONE)

    /**
     * Check if this permanent has any counters.
     */
    val hasAnyCounters: Boolean
        get() = counters.values.any { it > 0 }

    // ==========================================================================
    // Attachment Accessors
    // ==========================================================================

    /**
     * Check if this is an aura or equipment currently attached to something.
     */
    val isAttached: Boolean
        get() = attachedTo != null

    /**
     * Check if this permanent has anything attached to it.
     */
    val hasAttachments: Boolean
        get() = attachments.isNotEmpty()

    /**
     * Get the number of attachments on this permanent.
     */
    val attachmentCount: Int
        get() = attachments.size

    companion object {
        /**
         * Create a base view from a card definition before any modifiers.
         * This is the starting point for the projection.
         */
        fun fromDefinition(
            entityId: EntityId,
            definition: CardDefinition,
            ownerId: EntityId,
            controllerId: EntityId,
            isTapped: Boolean = false,
            hasSummoningSickness: Boolean = false,
            damage: Int = 0,
            counters: Map<CounterType, Int> = emptyMap(),
            attachedTo: EntityId? = null,
            attachments: List<EntityId> = emptyList()
        ): GameObjectView {
            val types = mutableSetOf<CardType>()
            if (definition.typeLine.isCreature) types.add(CardType.CREATURE)
            if (definition.typeLine.isLand) types.add(CardType.LAND)
            if (definition.typeLine.isArtifact) types.add(CardType.ARTIFACT)
            if (definition.typeLine.isEnchantment) types.add(CardType.ENCHANTMENT)
            if (definition.typeLine.isInstant) types.add(CardType.INSTANT)
            if (definition.typeLine.isSorcery) types.add(CardType.SORCERY)
            if (CardType.PLANESWALKER in definition.typeLine.cardTypes) types.add(CardType.PLANESWALKER)

            // Derive loyalty from counters for planeswalkers
            val isPlaneswalker = CardType.PLANESWALKER in types
            val loyalty = if (isPlaneswalker) counters[CounterType.LOYALTY] else null

            return GameObjectView(
                entityId = entityId,
                name = definition.name,
                controllerId = controllerId,
                ownerId = ownerId,
                types = types,
                subtypes = definition.typeLine.subtypes,
                colors = definition.manaCost.colors,
                keywords = definition.keywords,
                power = definition.creatureStats?.basePower,
                toughness = definition.creatureStats?.baseToughness,
                hasAbilities = true,
                damage = damage,
                isTapped = isTapped,
                hasSummoningSickness = hasSummoningSickness,
                counters = counters,
                attachedTo = attachedTo,
                attachments = attachments,
                loyalty = loyalty,
                cantBlock = false
            )
        }
    }
}

/**
 * Mutable builder for constructing a GameObjectView during projection.
 * Used internally by the StateProjector.
 */
class GameObjectViewBuilder(
    val entityId: EntityId,
    var name: String,
    var controllerId: EntityId,
    val ownerId: EntityId,
    val types: MutableSet<CardType>,
    val subtypes: MutableSet<Subtype>,
    val colors: MutableSet<Color>,
    val keywords: MutableSet<Keyword>,
    var power: Int?,
    var toughness: Int?,
    var hasAbilities: Boolean = true,
    var damage: Int = 0,
    var isTapped: Boolean = false,
    var hasSummoningSickness: Boolean = false,
    val counters: MutableMap<CounterType, Int> = mutableMapOf(),
    var attachedTo: EntityId? = null,
    val attachments: MutableList<EntityId> = mutableListOf(),
    var loyalty: Int? = null,
    var cantBlock: Boolean = false
) {
    /**
     * Build the final immutable view.
     */
    fun build(): GameObjectView {
        // Derive loyalty from counters for planeswalkers if not explicitly set
        val isPlaneswalker = CardType.PLANESWALKER in types
        val finalLoyalty = if (isPlaneswalker) {
            loyalty ?: counters[CounterType.LOYALTY]
        } else null

        return GameObjectView(
            entityId = entityId,
            name = name,
            controllerId = controllerId,
            ownerId = ownerId,
            types = types.toSet(),
            subtypes = subtypes.toSet(),
            colors = colors.toSet(),
            keywords = if (hasAbilities) keywords.toSet() else emptySet(),
            power = power,
            toughness = toughness,
            hasAbilities = hasAbilities,
            damage = damage,
            isTapped = isTapped,
            hasSummoningSickness = hasSummoningSickness,
            counters = counters.toMap(),
            attachedTo = attachedTo,
            attachments = attachments.toList(),
            loyalty = finalLoyalty,
            cantBlock = cantBlock
        )
    }

    companion object {
        /**
         * Create a builder from a base view.
         */
        fun from(view: GameObjectView): GameObjectViewBuilder = GameObjectViewBuilder(
            entityId = view.entityId,
            name = view.name,
            controllerId = view.controllerId,
            ownerId = view.ownerId,
            types = view.types.toMutableSet(),
            subtypes = view.subtypes.toMutableSet(),
            colors = view.colors.toMutableSet(),
            keywords = view.keywords.toMutableSet(),
            power = view.power,
            toughness = view.toughness,
            hasAbilities = view.hasAbilities,
            damage = view.damage,
            isTapped = view.isTapped,
            hasSummoningSickness = view.hasSummoningSickness,
            counters = view.counters.toMutableMap(),
            attachedTo = view.attachedTo,
            attachments = view.attachments.toMutableList(),
            loyalty = view.loyalty,
            cantBlock = view.cantBlock
        )
    }
}
