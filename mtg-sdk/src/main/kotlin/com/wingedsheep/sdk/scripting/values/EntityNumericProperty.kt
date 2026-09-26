package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
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

    /** Actual damage dealt by this object this turn, after prevention and replacement effects.
     * For battlefield permanents and resolving spells; includes damage to any recipient.
     * Zone changes reset the history. Damage from a departed source does not mark its new object.
     */
    @SerialName("DamageDealtThisTurn")
    @Serializable
    data object DamageDealtThisTurn : EntityNumericProperty {
        override val description: String = "damage dealt this turn"
    }

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

    /**
     * The entity's printed **base** power — its power before counters, Auras, Equipment, anthems,
     * and any other continuous modification. Reads the fixed
     * [com.wingedsheep.sdk.model.CreatureStats.basePower] assigned when the card was printed; a
     * creature with characteristic-defining `*`/CDA power has no fixed base and reads 0.
     *
     * This is the *same* notion of "base power" that [com.wingedsheep.sdk.scripting.predicates
     * .CardPredicate.PowerGreaterThanBase] tests against, so pairing the two stays consistent:
     * `Subtract(EntityProperty(e, Power), EntityProperty(e, BasePower))` yields exactly the
     * "difference" (current power − base power) that filter's qualifying creatures have — the
     * per-creature +1/+1 counter amount for Sovereign Okinec Ahau.
     */
    @SerialName("BasePower")
    @Serializable
    data object BasePower : EntityNumericProperty {
        override val description: String = "base power"
    }

    /**
     * The entity's printed **base** toughness — the toughness sibling of [BasePower]. Reads the
     * fixed [com.wingedsheep.sdk.model.CreatureStats.baseToughness]; `*`/CDA toughness reads 0.
     */
    @SerialName("BaseToughness")
    @Serializable
    data object BaseToughness : EntityNumericProperty {
        override val description: String = "base toughness"
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

    /**
     * The number the entity's controller chose as it entered the battlefield — Nameless Race's
     * "the life paid as it entered", read back by its characteristic-defining power and toughness.
     * Zero for a permanent that recorded no such choice.
     */
    @SerialName("ValueChosenAsEntered")
    @Serializable
    data object ValueChosenAsEntered : EntityNumericProperty {
        override val description: String = "the amount chosen as it entered"
    }

    /** The number of [counterType] counters on the entity — of every kind when `null`. */
    @SerialName("CounterCount")
    @Serializable
    data class CounterCount(val counterType: CounterType?) : EntityNumericProperty {
        override val description: String =
            counterType?.let { "the number of ${it.printed} counters" } ?: "the number of counters"
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
     * reduction reads `EntityProperty(EffectTarget.ContextTarget(0), ColorCount)`.
     */
    @SerialName("ColorCount")
    @Serializable
    data object ColorCount : EntityNumericProperty {
        override val description: String = "the number of its colors"
    }

    /**
     * The number of mana symbols of [colors] in **this one entity's** printed mana cost —
     * `{1}{U}{U}` counts 2 blue, `{U/R}{R}` counts 1 blue. Delegates to
     * [com.wingedsheep.sdk.core.ManaCost.coloredSymbolCount], so hybrid and Phyrexian pips count
     * for their color(s) (CR 107.4e/f) and a pip that is two of the requested colors is counted
     * once. Reads the *printed* cost — a card's mana cost is the mana symbols printed on it
     * (CR 202.1/202.1a), while additional costs, cost increases and cost reductions only produce
     * the spell's *total cost* (CR 601.2f), so none of them change what this counts; `{X}` is a
     * generic symbol (CR 107.4b) and counts nothing whatever value was announced for it. A
     * face-down object has no mana cost and counts 0 (CR 708.2a).
     *
     * The per-object counterpart of [com.wingedsheep.sdk.scripting.values.DynamicAmount.DevotionTo],
     * which counts the same symbols but across every permanent a player controls (CR 700.5). Do not
     * reach for `DevotionTo` when the card says "in its mana cost" — the scopes are different.
     *
     * Namor the Sub-Mariner: "Whenever you cast a noncreature spell with one or more blue mana
     * symbols in its mana cost, create that many 1/1 blue Merfolk creature tokens" is
     * `EntityProperty(EffectTarget.TriggeringEntity, ColoredManaSymbolCount(listOf(Color.BLUE)))`. The
     * matching filter side is
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ColoredManaSymbolsAtLeast].
     */
    @SerialName("ColoredManaSymbolCount")
    @Serializable
    data class ColoredManaSymbolCount(val colors: List<Color>) : EntityNumericProperty {
        override val description: String =
            "the number of ${colors.joinToString(" or ") { it.displayName.lowercase() }} " +
                "mana symbols in its mana cost"
    }

    /**
     * The excess damage (CR 120.4a) currently marked on this entity: `max(0, marked − toughness)`,
     * read from post-damage state. This is the amount-valued twin of the
     * [com.wingedsheep.sdk.scripting.conditions.TargetMarkedDamageExceedsToughness] condition.
     *
     * Read it AFTER a deal-damage step in the same composite/pipeline resolution, so the marked
     * damage in scope is the damage that step just dealt — e.g. Hell to Pay: "deals X damage to
     * target creature. Create a number of tapped Treasure tokens equal to the amount of excess
     * damage dealt to that creature this way." `EntityProperty(EffectTarget.ContextTarget(0),
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

    /**
     * The total N across this entity's instances of a numeric keyword ("bushido N", "toxic N") —
     * Takeno, Samurai General's "for each point of bushido it has". Instances add: a creature with
     * bushido 1 and bushido 2 has three points of bushido.
     *
     * Reads the *printed* N values, gated on the keyword surviving projection: a permanent that has
     * lost all abilities (layer 6) has no bushido and counts 0, and a face-down permanent counts 0.
     * Numeric keywords granted in the projected `<KEYWORD>_<n>` form (granted toxic) add their N
     * too. A keyword granted without an N carries no value — there is no such grant for bushido.
     *
     * Keywords are settled in layer 6, before every P/T layer, so a layer-7 static fed this
     * amount (`EntityProperty(AffectedEntity, KeywordValue(BUSHIDO))`) never depends on itself.
     */
    @SerialName("KeywordValue")
    @Serializable
    data class KeywordValue(val keyword: Keyword) : EntityNumericProperty {
        override val description: String = "points of ${keyword.displayName.lowercase()} it has"
    }
}

/**
 * Which kind of attachment [EntityNumericProperty.AttachmentCount] counts. The set is the
 * small closed family of MTG attachment types; [ANY] preserves the original
 * "all attachments" semantics.
 */
@Serializable
enum class AttachmentKind { ANY, EQUIPMENT, AURA }
