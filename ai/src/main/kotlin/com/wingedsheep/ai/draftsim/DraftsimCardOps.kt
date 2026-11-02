package com.wingedsheep.ai.draftsim

import kotlin.math.floor
import kotlin.math.min

/**
 * The pure per-card primitives the Draftsim port reads through (`SPEC_scoring.md` §3): rating
 * lookups, type-line predicates, color/cmc derivation. Bound to one pool's [tables]; no mutable
 * state. Shared by [DraftsimScorer] (card scoring) and [DraftsimDeckScorer]/[DraftsimDeckBuilder]
 * (deck scoring & construction) so the join + classification rules live in exactly one place.
 */
internal class DraftsimCardOps(val tables: DraftsimSetTables) {

    val ratings get() = tables.ratings
    val noRatings get() = ratings.isEmpty()

    /** rarity → rating fallback (`nW` `o_`). */
    private val rarityRating = mapOf("mythic" to 4.5, "rare" to 3.5, "uncommon" to 2.5, "common" to 1.5)

    /** `rating(name)` (`Wt`). */
    fun rating(name: String): Double? = ratings[DraftsimData.nameKey(name)]

    /** `ratingOrDefault(card)` (`Gu`): rating, else 4 for mythics, else 0. */
    fun ratingOrDefault(card: ScorerCard): Double = rating(card.name) ?: if (card.rarity == "mythic") 4.0 else 0.0

    /** `ratingFallback(card)` (`nW`): rating, else the rarity ladder (common floor). */
    fun ratingFallback(card: ScorerCard): Double = rating(card.name) ?: rarityRating[card.rarity] ?: rarityRating.getValue("common")

    private fun typeLine(card: ScorerCard) = card.typeLine.lowercase()
    fun isLand(card: ScorerCard) = typeLine(card).contains("land")
    fun isBasic(card: ScorerCard) = typeLine(card).contains("basic land")
    fun isCreature(card: ScorerCard) = typeLine(card).contains("creature")
    fun isLegendaryCreature(card: ScorerCard) = typeLine(card).let { it.contains("legendary") && it.contains("creature") }
    fun isPermanent(card: ScorerCard) = typeLine(card).let { t ->
        listOf("creature", "planeswalker", "artifact", "enchantment", "battle").any { t.contains(it) }
    }

    /** `cmcBucket(x)` (`ki`): `min(floor(x), 6)`. */
    fun cmcBucket(x: Double): Int = min(floor(x).toInt(), 6)

    /** `colors(card)` (`jt`): declared colors if present, else parsed from the cost. */
    fun colorsOf(card: ScorerCard): List<String> =
        if (card.colors.isNotEmpty()) card.colors else DraftsimMana.colorsInCost(card.manaCost)

    /** 0/1 vector over `eW` for a card's colors. */
    fun colorVector(colors: List<String>): IntArray {
        val v = IntArray(5)
        for (c in colors) idx(c).let { if (it >= 0) v[it] = 1 }
        return v
    }

    fun idx(color: String) = DraftsimMana.COLORS.indexOf(color)
    fun archRecord(name: String): DraftsimArchetypeRecord? = tables.archetypes[DraftsimData.nameKey(name)]

    /**
     * The colours [card] fixes: the archetype table's curated list where it has one, else the card's
     * own structural reading ([ScorerCard.fixing]); a land neither reads falls back to its colours or
     * identity. Empty for a nonland that fixes nothing.
     */
    fun fixingOf(card: ScorerCard): List<String> =
        archRecord(card.name)?.fixing?.takeIf { it.isNotEmpty() }
            ?: card.fixing.takeIf { it.isNotEmpty() }
            ?: if (isLand(card)) colorsOf(card).ifEmpty { card.colorIdentity } else emptyList()

    /**
     * How much [card] fixes a deck in [colors]: 1 for a land that makes two of them (or four or more
     * colours — a fetch for any basic), half that for a nonland fixer, which costs a card and usually
     * mana; 0 otherwise. Basics never count.
     */
    fun fixerWeight(card: ScorerCard, colors: Collection<String>): Double {
        if (isBasic(card)) return 0.0
        val fix = fixingOf(card)
        val covers = fix.size >= 4 || fix.count { it in colors } >= 2
        return when {
            !covers -> 0.0
            isLand(card) -> 1.0
            else -> NONLAND_FIXER_WEIGHT
        }
    }

    /** Per-card weight used by `wa`/`s_`: `rating ?? 2.5`, or 1 when the set ships no ratings. */
    fun deckCardWeight(card: ScorerCard): Double = if (noRatings) 1.0 else (rating(card.name) ?: 2.5)

    /** A card whose `ratingOrDefault` clears the bomb cutoff (`jm`'s `isBomb`). */
    fun isBombCard(card: ScorerCard): Boolean = ratingOrDefault(card) >= BOMB

    companion object {
        const val BOMB = 3.9

        /** A nonland fixer (Prophetic Prism, Rampant Growth, a Treasure maker) counts for half a fixing land. */
        const val NONLAND_FIXER_WEIGHT = 0.5
    }
}
