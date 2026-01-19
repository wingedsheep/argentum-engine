package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * A modifier represents a continuous effect that modifies game objects.
 *
 * Modifiers are collected from all sources (static abilities, auras, equipment,
 * global effects, etc.) and applied in layer order by the StateProjector.
 *
 * @property layer The layer in which this modifier applies
 * @property sourceId The entity that is the source of this modifier
 * @property timestamp When this modifier was created (for ordering within layer)
 * @property modification The actual modification to apply
 * @property filter Optional filter for which entities this modifier affects
 */
@Serializable
data class Modifier(
    val layer: Layer,
    val sourceId: EntityId,
    val timestamp: Long,
    val modification: Modification,
    val filter: ModifierFilter = ModifierFilter.Self
) {
    companion object {
        private var timestampCounter = 0L

        /**
         * Generate a new timestamp for modifier ordering.
         */
        fun nextTimestamp(): Long = ++timestampCounter

        /**
         * Reset timestamp counter (for testing).
         */
        fun resetTimestamps() {
            timestampCounter = 0L
        }
    }
}

/**
 * The actual modification to apply to a game object.
 *
 * Each modification type corresponds to a specific layer and defines
 * what changes to make to the affected entities.
 */
@Serializable
sealed interface Modification {
    // =========================================================================
    // Layer 2: Control-changing
    // =========================================================================

    /**
     * Change control of a permanent to a different player.
     */
    @Serializable
    data class ChangeControl(val newControllerId: EntityId) : Modification

    // =========================================================================
    // Layer 4: Type-changing
    // =========================================================================

    /**
     * Add a card type to an object.
     */
    @Serializable
    data class AddType(val type: CardType) : Modification

    /**
     * Remove a card type from an object.
     */
    @Serializable
    data class RemoveType(val type: CardType) : Modification

    /**
     * Add a subtype to an object.
     */
    @Serializable
    data class AddSubtype(val subtype: Subtype) : Modification

    /**
     * Remove a subtype from an object.
     */
    @Serializable
    data class RemoveSubtype(val subtype: Subtype) : Modification

    /**
     * Set all subtypes to a specific set (removes existing subtypes).
     */
    @Serializable
    data class SetSubtypes(val subtypes: Set<Subtype>) : Modification

    // =========================================================================
    // Layer 5: Color-changing
    // =========================================================================

    /**
     * Add a color to an object.
     */
    @Serializable
    data class AddColor(val color: Color) : Modification

    /**
     * Remove a color from an object.
     */
    @Serializable
    data class RemoveColor(val color: Color) : Modification

    /**
     * Set all colors (removes existing colors).
     */
    @Serializable
    data class SetColors(val colors: Set<Color>) : Modification

    // =========================================================================
    // Layer 6: Ability-adding/removing
    // =========================================================================

    /**
     * Add a keyword ability.
     */
    @Serializable
    data class AddKeyword(val keyword: Keyword) : Modification

    /**
     * Remove a keyword ability.
     */
    @Serializable
    data class RemoveKeyword(val keyword: Keyword) : Modification

    /**
     * Remove all abilities (like Humility).
     */
    @Serializable
    data object RemoveAllAbilities : Modification

    /**
     * Add a restriction that prevents blocking.
     */
    @Serializable
    data object AddCantBlockRestriction : Modification

    /**
     * Makes a creature assign combat damage equal to its toughness rather than power.
     * @property onlyWhenToughnessGreaterThanPower If true, only applies when toughness > power
     */
    @Serializable
    data class AssignDamageEqualToToughness(
        val onlyWhenToughnessGreaterThanPower: Boolean = true
    ) : Modification

    // =========================================================================
    // Layer 7a: P/T from CDAs
    // =========================================================================

    /**
     * Set P/T based on a characteristic-defining ability.
     * The function will be evaluated during projection.
     */
    @Serializable
    data class SetPTFromCDA(
        val cdaType: CDAType
    ) : Modification

    // =========================================================================
    // Layer 7b: P/T setting
    // =========================================================================

    /**
     * Set power and toughness to specific values.
     */
    @Serializable
    data class SetPT(val power: Int, val toughness: Int) : Modification

    /**
     * Set power only.
     */
    @Serializable
    data class SetPower(val power: Int) : Modification

    /**
     * Set toughness only.
     */
    @Serializable
    data class SetToughness(val toughness: Int) : Modification

    // =========================================================================
    // Layer 7c: P/T modification
    // =========================================================================

    /**
     * Modify power and toughness by delta values.
     */
    @Serializable
    data class ModifyPT(val powerDelta: Int, val toughnessDelta: Int) : Modification

    /**
     * Modify power only.
     */
    @Serializable
    data class ModifyPower(val delta: Int) : Modification

    /**
     * Modify toughness only.
     */
    @Serializable
    data class ModifyToughness(val delta: Int) : Modification

    // =========================================================================
    // Layer 7e: P/T switching
    // =========================================================================

    /**
     * Switch power and toughness values.
     */
    @Serializable
    data object SwitchPT : Modification
}

/**
 * Types of characteristic-defining abilities for P/T.
 */
@Serializable
enum class CDAType {
    /**
     * P/T equal to number of cards in graveyard (e.g., Lhurgoyf).
     */
    CARDS_IN_GRAVEYARD,

    /**
     * P/T equal to number of creatures you control.
     */
    CREATURES_YOU_CONTROL,

    /**
     * P/T equal to number of lands you control.
     */
    LANDS_YOU_CONTROL,

    /**
     * P/T equal to cards in hand.
     */
    CARDS_IN_HAND,

    /**
     * P/T equal to devotion to a color (requires context).
     */
    DEVOTION,

    /**
     * Custom CDA (script will handle).
     */
    CUSTOM
}

/**
 * Filter for determining which entities a modifier affects.
 */
@Serializable
sealed interface ModifierFilter {
    /**
     * Affects only the source entity itself.
     */
    @Serializable
    data object Self : ModifierFilter

    /**
     * Affects the entity this is attached to (for auras/equipment).
     */
    @Serializable
    data object AttachedTo : ModifierFilter

    /**
     * Affects a specific entity.
     */
    @Serializable
    data class Specific(val entityId: EntityId) : ModifierFilter

    /**
     * Affects all entities matching criteria.
     */
    @Serializable
    data class All(val criteria: EntityCriteria) : ModifierFilter

    /**
     * Affects entities controlled by a specific player.
     */
    @Serializable
    data class ControlledBy(val playerId: EntityId) : ModifierFilter

    /**
     * Affects entities NOT controlled by source's controller.
     */
    @Serializable
    data object Opponents : ModifierFilter
}

/**
 * Criteria for filtering entities.
 */
@Serializable
sealed interface EntityCriteria {
    @Serializable
    data object Creatures : EntityCriteria

    @Serializable
    data object Lands : EntityCriteria

    @Serializable
    data object Artifacts : EntityCriteria

    @Serializable
    data object Enchantments : EntityCriteria

    @Serializable
    data object Permanents : EntityCriteria

    @Serializable
    data class WithKeyword(val keyword: Keyword) : EntityCriteria

    @Serializable
    data class WithSubtype(val subtype: Subtype) : EntityCriteria

    @Serializable
    data class WithColor(val color: Color) : EntityCriteria

    @Serializable
    data class And(val criteria: List<EntityCriteria>) : EntityCriteria

    @Serializable
    data class Or(val criteria: List<EntityCriteria>) : EntityCriteria

    @Serializable
    data class Not(val criteria: EntityCriteria) : EntityCriteria
}
