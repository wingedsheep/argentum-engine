package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.*

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
 * This class reads values from projected components (ProjectedPTComponent,
 * ProjectedAbilitiesComponent, etc.) which are populated by the StateProjector
 * during the projection process.
 *
 * Usage:
 * ```kotlin
 * val projector = StateProjector.forState(state, provider)
 * val view = projector.getView(entityId)
 *
 * // Access values
 * println(view.power)          // reads from ProjectedPTComponent
 * println(view.keywords)       // reads from ProjectedAbilitiesComponent
 * println(view.hasKeyword(Keyword.FLYING))
 *
 * // Access new components directly
 * val customComponent = view.get<MyCustomProjectedComponent>()
 * ```
 *
 * @property container The projected ComponentContainer with all modifications applied
 * @property entityId The entity this view represents
 * @property attachments Entities attached to this permanent (computed by StateProjector)
 */
class GameObjectView(
    private val container: ComponentContainer,
    val entityId: EntityId,
    val attachments: List<EntityId> = emptyList()
) {
    // =========================================================================
    // Identity accessors (from base container)
    // =========================================================================

    /**
     * The entity's name.
     */
    val name: String
        get() = container.get<CardComponent>()?.definition?.name ?: ""

    /**
     * The entity's owner (cannot be changed by effects).
     */
    val ownerId: EntityId
        get() = container.get<CardComponent>()?.ownerId ?: EntityId("unknown")

    /**
     * The entity's current controller (after Layer 2 effects).
     */
    val controllerId: EntityId
        get() = container.get<ProjectedControlComponent>()?.controllerId
            ?: container.get<ControllerComponent>()?.controllerId
            ?: ownerId

    // =========================================================================
    // Type accessors (Layer 4)
    // =========================================================================

    /**
     * Current card types (after Layer 4 effects).
     */
    val types: Set<CardType>
        get() = container.get<ProjectedTypesComponent>()?.types
            ?: container.get<BaseTypesComponent>()?.types
            ?: emptySet()

    /**
     * Current subtypes (after Layer 4 effects).
     */
    val subtypes: Set<Subtype>
        get() = container.get<ProjectedTypesComponent>()?.subtypes
            ?: container.get<BaseTypesComponent>()?.subtypes
            ?: emptySet()

    val isCreature: Boolean get() = CardType.CREATURE in types
    val isLand: Boolean get() = CardType.LAND in types
    val isArtifact: Boolean get() = CardType.ARTIFACT in types
    val isEnchantment: Boolean get() = CardType.ENCHANTMENT in types
    val isInstant: Boolean get() = CardType.INSTANT in types
    val isSorcery: Boolean get() = CardType.SORCERY in types
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in types
    val isPermanent: Boolean get() = isCreature || isLand || isArtifact || isEnchantment || isPlaneswalker

    // =========================================================================
    // Color accessors (Layer 5)
    // =========================================================================

    /**
     * Current colors (after Layer 5 effects).
     */
    val colors: Set<Color>
        get() = container.get<ProjectedColorsComponent>()?.colors
            ?: container.get<BaseColorsComponent>()?.colors
            ?: emptySet()

    // =========================================================================
    // Ability accessors (Layer 6)
    // =========================================================================

    /**
     * Current keywords (after Layer 6 effects).
     */
    val keywords: Set<Keyword>
        get() {
            val abilities = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
            return if (abilities?.hasAbilities == true) abilities.keywords else emptySet()
        }

    /**
     * Whether the object still has its printed abilities.
     */
    val hasAbilities: Boolean
        get() = container.get<ProjectedAbilitiesComponent>()?.hasAbilities
            ?: true

    /**
     * Whether this creature is prevented from blocking.
     */
    val cantBlock: Boolean
        get() = container.get<ProjectedAbilitiesComponent>()?.cantBlock
            ?: false

    /**
     * Whether this creature assigns combat damage equal to toughness.
     */
    val assignsDamageEqualToToughness: Boolean
        get() = container.get<ProjectedAbilitiesComponent>()?.assignsDamageEqualToToughness
            ?: false

    /**
     * Check if this creature has a specific keyword.
     */
    fun hasKeyword(keyword: Keyword): Boolean = keyword in keywords

    // =========================================================================
    // P/T accessors (Layer 7)
    // =========================================================================

    /**
     * Current power (after Layer 7 effects), null for non-creatures.
     */
    val power: Int?
        get() = container.get<ProjectedPTComponent>()?.power
            ?: container.get<BaseStatsComponent>()?.power

    /**
     * Current toughness (after Layer 7 effects), null for non-creatures.
     */
    val toughness: Int?
        get() = container.get<ProjectedPTComponent>()?.toughness
            ?: container.get<BaseStatsComponent>()?.toughness

    // =========================================================================
    // State accessors (from base container)
    // =========================================================================

    /**
     * Current damage marked on this permanent.
     */
    val damage: Int
        get() = container.get<DamageComponent>()?.amount ?: 0

    /**
     * Whether this permanent is tapped.
     */
    val isTapped: Boolean
        get() = container.has<TappedComponent>()

    /**
     * Whether this creature has summoning sickness.
     */
    val hasSummoningSickness: Boolean
        get() = container.has<SummoningSicknessComponent>()

    /**
     * All counters on this permanent.
     */
    val counters: Map<CounterType, Int>
        get() = container.get<CountersComponent>()?.counters ?: emptyMap()

    /**
     * The entity this is attached to (for auras/equipment).
     */
    val attachedTo: EntityId?
        get() = container.get<AttachedToComponent>()?.targetId

    /**
     * Current loyalty for planeswalkers (derived from loyalty counters).
     */
    val loyalty: Int?
        get() = if (isPlaneswalker) counters[CounterType.LOYALTY] else null

    // =========================================================================
    // Derived properties
    // =========================================================================

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
     * Check if this creature can attack.
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

    // =========================================================================
    // Counter accessors
    // =========================================================================

    fun hasCounter(type: CounterType): Boolean = (counters[type] ?: 0) > 0
    fun getCounterCount(type: CounterType): Int = counters[type] ?: 0
    val plusOnePlusOneCounters: Int get() = getCounterCount(CounterType.PLUS_ONE_PLUS_ONE)
    val minusOneMinusOneCounters: Int get() = getCounterCount(CounterType.MINUS_ONE_MINUS_ONE)
    val hasAnyCounters: Boolean get() = counters.values.any { it > 0 }

    // =========================================================================
    // Attachment accessors
    // =========================================================================

    val isAttached: Boolean get() = attachedTo != null
    val hasAttachments: Boolean get() = attachments.isNotEmpty()
    val attachmentCount: Int get() = attachments.size

    // =========================================================================
    // Generic component accessor
    // =========================================================================

    /**
     * Get a component by type from the projected container.
     *
     * This allows accessing new custom components that aren't part of the
     * standard GameObjectView interface.
     */
    fun <T : Component> get(type: kotlin.reflect.KClass<T>): T? = container.get(type)
}
