package com.wingedsheep.rulesengine.ecs.layers

import kotlinx.serialization.Serializable

/**
 * MTG Layer system for applying continuous effects (Rule 613).
 *
 * Continuous effects are applied in a specific order to ensure consistent
 * and predictable game state. Each layer handles a specific type of modification.
 *
 * Within each layer, effects are applied in timestamp order (oldest first),
 * with dependency handling for effects that depend on each other.
 */
@Serializable
enum class Layer(val order: Int, val description: String) {
    /**
     * Layer 1: Copy effects.
     * Effects that cause an object to become a copy of another object.
     * Example: Clone, Vesuvan Doppelganger
     */
    COPY(1, "Copy effects"),

    /**
     * Layer 2: Control-changing effects.
     * Effects that change which player controls an object.
     * Example: Control Magic, Act of Treason
     */
    CONTROL(2, "Control-changing effects"),

    /**
     * Layer 3: Text-changing effects.
     * Effects that change the text of an object.
     * Example: Sleight of Mind, Mind Bend
     */
    TEXT(3, "Text-changing effects"),

    /**
     * Layer 4: Type-changing effects.
     * Effects that change card types, subtypes, or supertypes.
     * Example: Humility (removes creature types), Blood Moon (makes lands Mountains)
     */
    TYPE(4, "Type-changing effects"),

    /**
     * Layer 5: Color-changing effects.
     * Effects that change the color of an object.
     * Example: Painter's Servant, Darkest Hour
     */
    COLOR(5, "Color-changing effects"),

    /**
     * Layer 6: Ability-adding and ability-removing effects.
     * Effects that add or remove abilities from objects.
     * Example: Levitation (grants flying), Humility (removes abilities)
     */
    ABILITY(6, "Ability-adding/removing effects"),

    /**
     * Layer 7a: Characteristic-defining abilities that set P/T.
     * Effects from CDAs that define power/toughness based on game state.
     * Example: Tarmogoyf, Nightmare
     */
    PT_CDA(7, "P/T characteristic-defining abilities"),

    /**
     * Layer 7b: Effects that set P/T to specific values.
     * Effects that set power and/or toughness to a specific number.
     * Example: Humility (sets to 1/1), Turn to Frog (sets to 1/1)
     */
    PT_SET(8, "P/T setting effects"),

    /**
     * Layer 7c: Effects that modify P/T without setting.
     * Effects that add to or subtract from power and/or toughness.
     * Example: Giant Growth (+3/+3), Glorious Anthem (+1/+1 to your creatures)
     */
    PT_MODIFY(9, "P/T modification effects"),

    /**
     * Layer 7d: +1/+1 and -1/-1 counters.
     * Power/toughness changes from counters.
     */
    PT_COUNTERS(10, "P/T from counters"),

    /**
     * Layer 7e: Effects that switch power and toughness.
     * Example: Inside Out, About Face
     */
    PT_SWITCH(11, "P/T switching effects");

    /**
     * Check if this layer is a P/T sublayer (7a-7e).
     */
    val isPTLayer: Boolean
        get() = this in listOf(PT_CDA, PT_SET, PT_MODIFY, PT_COUNTERS, PT_SWITCH)

    /**
     * Check if this layer comes before another layer.
     */
    fun isBefore(other: Layer): Boolean = this.order < other.order

    /**
     * Check if this layer comes after another layer.
     */
    fun isAfter(other: Layer): Boolean = this.order > other.order

    companion object {
        /**
         * Get all layers in application order.
         */
        fun inOrder(): List<Layer> = entries.sortedBy { it.order }

        /**
         * Get all P/T sublayers (7a-7e).
         */
        fun ptLayers(): List<Layer> = listOf(PT_CDA, PT_SET, PT_MODIFY, PT_COUNTERS, PT_SWITCH)
    }
}
