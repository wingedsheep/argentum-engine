package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.Serializable

/**
 * A numeric property of a card that can be aggregated over.
 * Used with [DynamicAmount.AggregateBattlefield].
 */
@Serializable
enum class CardNumericProperty(val description: String) {
    MANA_VALUE("mana value"),
    POWER("power"),
    TOUGHNESS("toughness"),
    /**
     * Counters of every kind on the permanent — "the number of counters on permanents you control"
     * (Hydra Trainer). A single kind is [DynamicAmount.AggregateBattlefield.counterType] instead.
     */
    COUNTERS("counters")
}

/**
 * An aggregation function applied to a collection of battlefield entities.
 * Used with [DynamicAmount.AggregateBattlefield].
 */
@Serializable
enum class Aggregation {
    COUNT, MAX, MIN, SUM,
    /** Count distinct card types across all matched entities */
    DISTINCT_TYPES,
    /**
     * Count distinct *permanent* card types across all matched entities (CR 110.4: artifact,
     * battle, creature, enchantment, land, planeswalker). Like [DISTINCT_TYPES] but non-permanent
     * card types — instant, sorcery, and kindred (CR 300.2b: a kindred card is a permanent only
     * via its *other* type) — never contribute. Used for "N or more permanent types among …"
     * (Matzalantli, the Great Door).
     */
    DISTINCT_PERMANENT_TYPES,
    /** Count distinct colors across all matched entities */
    DISTINCT_COLORS,
    /**
     * Count the distinct *color pairs* contributed by the matched entities — one pair per entity
     * that is exactly two colors (CR 105.2c), the same pair on two entities counting once. There
     * are ten pairs in Magic, so the value is bounded by 10.
     *
     * Entities of one, three, four, or five colors — and colorless ones — contribute nothing:
     * "color pair" names an *exactly two colors* object, so the "that are exactly two colors"
     * clause is part of this aggregation rather than something the filter has to spell.
     * Used for "the number of different color pairs among permanents you control that are
     * exactly two colors" (Niv-Mizzet, Guildpact).
     */
    DISTINCT_COLOR_PAIRS,
    /** Count distinct English card names across all matched entities */
    DISTINCT_NAMES,
    /**
     * Count distinct basic land subtypes (Plains, Island, Swamp, Mountain, Forest)
     * across all matched entities. Used for the Domain ability word.
     * Bounded by 5; nonbasic lands with basic subtypes (e.g., Tundra → Plains+Island)
     * contribute each of their basic subtypes.
     */
    DISTINCT_BASIC_LAND_SUBTYPES,
    /**
     * Count distinct planeswalker types (CR 205.3j) across all matched entities that are
     * planeswalkers — "the number of planeswalker types among planeswalkers you control" (Tam, the
     * Possibility). Two Jaces count once; a planeswalker with no subtype contributes nothing.
     *
     * A planeswalker's subtypes are planeswalker types unless it has also become another card
     * type (an animated Gideon is also a Human Soldier creature, CR 205.3d), so creature types
     * are excluded rather than a fixed list of planeswalker types included: that list grows with
     * every set, and a stale copy would silently undercount a new walker.
     */
    DISTINCT_PLANESWALKER_SUBTYPES,
    /**
     * Count distinct kinds of counters across all matched entities — i.e. the number of
     * different [com.wingedsheep.sdk.core.CounterType]s present on at least one matched
     * permanent. A permanent with both +1/+1 and finality counters contributes two kinds;
     * the same kind on several permanents still counts once. Used for "different kinds of
     * counters among <group>" (e.g. Hundred-Battle Veteran).
     */
    DISTINCT_COUNTER_TYPES,
    /**
     * Count distinct values of the configured [CardNumericProperty] (power, toughness, or mana
     * value) across all matched entities — e.g. "the number of different powers among creatures
     * you control" (Selvala, Eager Trailblazer). Requires `property` to be set; two creatures with
     * the same power count once.
     */
    DISTINCT_VALUES
}
