package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.card.CardDefinition
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
 * in the EcsGameState remains unchanged; this is a calculated view.
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
    val hasSummoningSickness: Boolean = false
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
        get() = isCreature && !isTapped

    /**
     * Check if this creature has flying or reach (for blocking purposes).
     */
    val canBlockFlying: Boolean
        get() = hasKeyword(Keyword.FLYING) || hasKeyword(Keyword.REACH)

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
            damage: Int = 0
        ): GameObjectView {
            val types = mutableSetOf<CardType>()
            if (definition.typeLine.isCreature) types.add(CardType.CREATURE)
            if (definition.typeLine.isLand) types.add(CardType.LAND)
            if (definition.typeLine.isArtifact) types.add(CardType.ARTIFACT)
            if (definition.typeLine.isEnchantment) types.add(CardType.ENCHANTMENT)
            if (definition.typeLine.isInstant) types.add(CardType.INSTANT)
            if (definition.typeLine.isSorcery) types.add(CardType.SORCERY)
            if (CardType.PLANESWALKER in definition.typeLine.cardTypes) types.add(CardType.PLANESWALKER)

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
                hasSummoningSickness = hasSummoningSickness
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
    var hasSummoningSickness: Boolean = false
) {
    /**
     * Build the final immutable view.
     */
    fun build(): GameObjectView = GameObjectView(
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
        hasSummoningSickness = hasSummoningSickness
    )

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
            hasSummoningSickness = view.hasSummoningSickness
        )
    }
}
