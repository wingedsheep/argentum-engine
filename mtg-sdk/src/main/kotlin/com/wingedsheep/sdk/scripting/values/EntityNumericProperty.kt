package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A numeric property that can be read from an entity.
 *
 * Richer than [CardNumericProperty] — handles parameterized properties (counter type)
 * and entity-specific properties (blocker count, attachment count) that only make sense
 * when reading from a specific entity rather than aggregating over a group.
 */
@Serializable
sealed interface EntityNumericProperty {
    val description: String

    @SerialName("Power")
    @Serializable
    data object Power : EntityNumericProperty {
        override val description: String = "power"
    }

    @SerialName("Toughness")
    @Serializable
    data object Toughness : EntityNumericProperty {
        override val description: String = "toughness"
    }

    @SerialName("ManaValue")
    @Serializable
    data object ManaValue : EntityNumericProperty {
        override val description: String = "mana value"
    }

    /**
     * Total mana actually paid from the pool to cast a spell on the stack.
     * Sums every `manaSpent{Color}` bucket on the spell's `SpellOnStackComponent`.
     *
     * Differs from [ManaValue]: ManaValue is the printed cost (unaffected by cost
     * reductions or increases per CR 202.3; for {X} spells, 202.3e fixes the X
     * portion to the chosen value once the spell is on the stack). ManaSpent
     * reflects what was actually paid — so cost-reduced spells (affinity, convoke,
     * etc.) show less than their mana value here. Returns 0 if the entity is not
     * a spell on the stack.
     */
    @SerialName("ManaSpent")
    @Serializable
    data object ManaSpent : EntityNumericProperty {
        override val description: String = "the amount of mana spent to cast it"
    }

    @SerialName("CounterCount")
    @Serializable
    data class CounterCount(val counterType: CounterTypeFilter) : EntityNumericProperty {
        override val description: String = "the number of ${counterType.description} counters"
    }

    /**
     * The number of permanents attached to this entity, optionally narrowed to a single
     * [AttachmentKind]. [AttachmentKind.ANY] (the default) counts every attachment — Auras,
     * Equipment, and Fortifications alike — matching the historical behaviour;
     * [AttachmentKind.EQUIPMENT] / [AttachmentKind.AURA] count only that kind (Shagrat, Loot
     * Bearer: "X is the number of Equipment attached to Shagrat").
     */
    @SerialName("AttachmentCount")
    @Serializable
    data class AttachmentCount(val kind: AttachmentKind = AttachmentKind.ANY) : EntityNumericProperty {
        override val description: String = when (kind) {
            AttachmentKind.ANY -> "the number of Auras and Equipment attached"
            AttachmentKind.EQUIPMENT -> "the number of Equipment attached"
            AttachmentKind.AURA -> "the number of Auras attached"
        }
    }

    @SerialName("BlockerCount")
    @Serializable
    data object BlockerCount : EntityNumericProperty {
        override val description: String = "the number of creatures blocking"
    }

    /**
     * The number of distinct subtypes this entity has, read from projected state when
     * available (so layer-4 type-changing effects, including Changeling, are honored).
     *
     * Note: this counts every subtype string on the entity, not only creature types.
     * For creature cards without type-changing effects to non-creature subtypes (the
     * common case) this matches CR 205.3m's "creature types"; cards that gain artifact
     * or vehicle subtypes will be over-counted.
     */
    @SerialName("SubtypeCount")
    @Serializable
    data object SubtypeCount : EntityNumericProperty {
        override val description: String = "the number of its subtypes"
    }

    /**
     * The number of distinct colors this entity has, read from projected state when
     * available (so layer-5 color-changing effects are honored). A colorless entity
     * counts 0; a monocolored entity 1; a five-color entity 5 (CR 105.2 — there are
     * five colors).
     *
     * Powers "for each color of [entity]" amounts — e.g. Dragonfire Blade's equip cost
     * reduction reads `EntityProperty(EntityReference.Target(0), ColorCount)`.
     */
    @SerialName("ColorCount")
    @Serializable
    data object ColorCount : EntityNumericProperty {
        override val description: String = "the number of its colors"
    }

    /**
     * The excess damage (CR 120.4a) currently marked on this entity: `max(0, marked − toughness)`,
     * read from post-damage state. This is the amount-valued twin of the
     * [com.wingedsheep.sdk.scripting.conditions.TargetMarkedDamageExceedsToughness] condition.
     *
     * Read it AFTER a deal-damage step in the same composite/pipeline resolution, so the marked
     * damage in scope is the damage that step just dealt — e.g. Hell to Pay: "deals X damage to
     * target creature. Create a number of tapped Treasure tokens equal to the amount of excess
     * damage dealt to that creature this way." `EntityProperty(EntityReference.Target(0),
     * ExcessMarkedDamage)`. CompositeEffect resolves sub-effects sequentially with no interleaved
     * SBA pass, so for the canonical "deal N, then read excess" shape this equals "how much did
     * that deal-damage step push the target past lethal" — there is no other source of marked
     * damage in scope. Returns 0 if the entity is not a creature on the battlefield.
     */
    @SerialName("ExcessMarkedDamage")
    @Serializable
    data object ExcessMarkedDamage : EntityNumericProperty {
        override val description: String = "the excess damage dealt to it this way"
    }
}

/**
 * Which kind of attachment [EntityNumericProperty.AttachmentCount] counts. The set is the
 * small closed family of MTG attachment types; [ANY] preserves the original
 * "all attachments" semantics.
 */
@Serializable
enum class AttachmentKind { ANY, EQUIPMENT, AURA }
