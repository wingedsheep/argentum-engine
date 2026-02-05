package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.targeting.TargetRequirement
import kotlinx.serialization.Serializable

// =============================================================================
// Composite Effects
// =============================================================================

/**
 * Multiple effects that happen together.
 */
@Serializable
data class CompositeEffect(
    val effects: List<Effect>
) : Effect {
    override val description: String = effects.joinToString(". ") { it.description }
}

/**
 * Optional effect wrapper - the player may choose to perform or skip this effect.
 * "You may draw a card" or "You may shuffle your library"
 *
 * Use this to compose optional parts of abilities rather than creating
 * specific optional variants of each effect.
 */
@Serializable
data class MayEffect(
    val effect: Effect,
    val description_override: String? = null
) : Effect {
    override val description: String = description_override ?: "You may ${effect.description.lowercase()}"
}

/**
 * Represents a single mode in a modal spell.
 *
 * Each mode can have its own targeting requirements, allowing cards like
 * Cryptic Command where different modes need different targets.
 *
 * @property effect The effect when this mode is chosen
 * @property targetRequirements Targets required for this specific mode
 * @property description Human-readable description of the mode
 */
@Serializable
data class Mode(
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val description: String = effect.description
) {
    companion object {
        /**
         * Create a mode with no targeting.
         */
        fun noTarget(effect: Effect, description: String = effect.description): Mode =
            Mode(effect, emptyList(), description)

        /**
         * Create a mode with a single target.
         */
        fun withTarget(effect: Effect, target: TargetRequirement, description: String = effect.description): Mode =
            Mode(effect, listOf(target), description)
    }
}

/**
 * Modal spell effect - choose one or more of several modes.
 * "Choose one — [Mode A] or [Mode B]"
 * "Choose two — [Mode A], [Mode B], [Mode C], or [Mode D]"
 *
 * Each mode can have its own targeting requirements, which are combined
 * based on which modes are chosen when the spell is cast.
 *
 * Example (Cryptic Command):
 * ```kotlin
 * ModalEffect(
 *     modes = listOf(
 *         Mode.withTarget(CounterSpellEffect, TargetSpell(), "Counter target spell"),
 *         Mode.withTarget(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand), TargetPermanent(), "Return target permanent to its owner's hand"),
 *         Mode.noTarget(TapAllCreaturesEffect(CreatureGroupFilter.OpponentsControl), "Tap all creatures your opponents control"),
 *         Mode.noTarget(DrawCardsEffect(1), "Draw a card")
 *     ),
 *     chooseCount = 2
 * )
 * ```
 *
 * @property modes List of possible modes to choose from
 * @property chooseCount How many modes to choose (default 1)
 */
@Serializable
data class ModalEffect(
    val modes: List<Mode>,
    val chooseCount: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        when (chooseCount) {
            1 -> append("one")
            2 -> append("two")
            3 -> append("three")
            else -> append(chooseCount)
        }
        append(" —\n")
        modes.forEachIndexed { index, mode ->
            append("• ")
            append(mode.description)
            if (index < modes.lastIndex) append("\n")
        }
    }

    companion object {
        /**
         * Create a simple modal effect with effects that have no targeting.
         * Backwards compatible with the old List<Effect> pattern.
         */
        fun simple(effects: List<Effect>, chooseCount: Int = 1): ModalEffect =
            ModalEffect(effects.map { Mode.noTarget(it) }, chooseCount)

        /**
         * Create a choose-one modal effect.
         */
        fun chooseOne(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 1)

        /**
         * Create a choose-two modal effect (Cryptic Command style).
         */
        fun chooseTwo(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 2)
    }
}

/**
 * Effect with an optional cost - "You may [cost]. If you do, [ifPaid]."
 *
 * This is the fundamental building block for optional effects like:
 * - "You may pay {2}. If you do, draw a card."
 * - "You may sacrifice a creature. If you do, deal 3 damage to any target."
 * - "You may discard a card. If you do, draw two cards."
 *
 * @property cost The optional cost the player may pay (e.g., PayLifeEffect, SacrificeEffect)
 * @property ifPaid The effect that happens if the player pays the cost
 * @property ifNotPaid Optional effect if the player doesn't pay (usually null)
 */
@Serializable
data class OptionalCostEffect(
    val cost: Effect,
    val ifPaid: Effect,
    val ifNotPaid: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("You may ${cost.description.replaceFirstChar { it.lowercase() }}. ")
        append("If you do, ${ifPaid.description.replaceFirstChar { it.lowercase() }}")
        if (ifNotPaid != null) {
            append(". Otherwise, ${ifNotPaid.description.replaceFirstChar { it.lowercase() }}")
        }
    }
}

/**
 * Reflexive trigger - "When you do, [effect]."
 *
 * Used for abilities that trigger from the resolution of another effect.
 * Example: Heart-Piercer Manticore - "You may sacrifice another creature.
 *          When you do, deal damage equal to that creature's power."
 *
 * @property action The optional action (sacrifice, discard, etc.)
 * @property optional Whether the action is optional
 * @property reflexiveEffect The effect that happens "when you do"
 */
@Serializable
data class ReflexiveTriggerEffect(
    val action: Effect,
    val optional: Boolean = true,
    val reflexiveEffect: Effect
) : Effect {
    override val description: String = buildString {
        if (optional) append("You may ")
        append(action.description.replaceFirstChar { it.lowercase() })
        append(". When you do, ")
        append(reflexiveEffect.description.replaceFirstChar { it.lowercase() })
    }
}

/**
 * Generic "unless" effect for punisher mechanics.
 * "Do [suffer], unless you [cost]."
 *
 * This is a unified effect that handles:
 * - "Sacrifice this unless you discard a land card" (Thundering Wurm)
 * - "Sacrifice this unless you sacrifice three Forests" (Primeval Force)
 * - Similar punisher-style effects
 *
 * @property cost The cost that can be paid to avoid the consequence
 * @property suffer The consequence if the cost is not paid
 * @property player Who must make the choice (defaults to controller)
 */
@Serializable
data class PayOrSufferEffect(
    val cost: PayCost,
    val suffer: Effect,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${suffer.description} unless you ${cost.description}"
}

/**
 * Effect that stores the result of executing an inner effect.
 *
 * This enables Oblivion Ring-style effects where the first trigger
 * needs to remember which card it exiled so the second trigger
 * can return it.
 *
 * @param effect The effect to execute
 * @param storeAs The variable to store the result in
 */
@Serializable
data class StoreResultEffect(
    val effect: Effect,
    val storeAs: EffectVariable
) : Effect {
    override val description: String = "${effect.description} (stored as ${storeAs.name})"
}

/**
 * Effect that stores a count from the result of executing an effect.
 *
 * Used for variable-count effects like Scapeshift:
 * "Sacrifice any number of lands. Search for that many land cards."
 *
 * @param effect The effect to execute (typically a sacrifice or similar)
 * @param storeAs The count variable to store the number in
 */
@Serializable
data class StoreCountEffect(
    val effect: Effect,
    val storeAs: EffectVariable.Count
) : Effect {
    override val description: String = "${effect.description} (count stored as ${storeAs.name})"
}

/**
 * Blight effect - "may blight N. If you do, [effect]"
 * Blight N means "put N -1/-1 counters on a creature you control".
 * This is an optional cost-gated effect used in triggered abilities.
 *
 * The player may choose a creature they control to blight. If they do,
 * the inner effect happens. If they don't (or can't), nothing happens.
 *
 * @property blightAmount Number of -1/-1 counters to place
 * @property innerEffect The effect that happens if the player blights
 * @property targetId The creature chosen to receive the counters (filled in during resolution)
 */
@Serializable
data class BlightEffect(
    val blightAmount: Int,
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may blight $blightAmount. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }
}

/**
 * "May tap another untapped creature you control. If you do, [effect]."
 * This is an optional cost-gated effect - the player may pay the cost to get the effect.
 *
 * @property innerEffect The effect that happens if the player pays the tap cost
 * @property targetId The creature chosen to tap (filled in during resolution)
 */
@Serializable
data class TapCreatureForEffectEffect(
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may tap another untapped creature you control. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }
}
