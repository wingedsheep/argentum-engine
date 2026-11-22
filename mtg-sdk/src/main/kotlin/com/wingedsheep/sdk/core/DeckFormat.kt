package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Constructed deck-construction formats whose legality data is sourced from Scryfall.
 *
 * A card's [com.wingedsheep.sdk.model.CardDefinition.legalFormats] is the set of formats in
 * which the card is currently legal (i.e. not banned and printed in a legal set). Format-
 * specific deck-construction rules beyond per-card legality (Commander singleton/100, Vintage
 * restricted-list, Canadian Highlander points, etc.) are enforced separately by the deck validator.
 *
 * The two Highlander formats are the exception to "sourced from Scryfall": Scryfall publishes no
 * legality for them, so their per-card legality is derived from Vintage's (see [scryfallKey]).
 */
@Serializable
enum class DeckFormat {
    STANDARD,
    PIONEER,
    MODERN,
    LEGACY,
    VINTAGE,
    COMMANDER,
    BRAWL,            // 100-card singleton (Arena/Historic Brawl). Scryfall key: "brawl".
    STANDARD_BRAWL,   // 60-card singleton, Standard pool.            Scryfall key: "standardbrawl".
    PAUPER,
    PREMODERN,

    /**
     * 100-card-minimum singleton, 1v1, 20 life, no commander, over the Vintage card pool (every
     * card Vintage allows, restricted ones included — a singleton deck can't exceed the restricted
     * list's one copy). No points list: that is [CANADIAN_HIGHLANDER].
     */
    HIGHLANDER,

    /**
     * Canadian Highlander (<https://www.canadianhighlander.ca/>): [HIGHLANDER]'s shape plus the
     * format's points list — powerful cards cost points and a deck may spend at most
     * [CanadianHighlanderPoints.POINT_CAP]. No sideboards.
     */
    CANADIAN_HIGHLANDER,

    /**
     * Primordial (<https://primordialformat.com/rules>): 40-card-minimum decks built from a single
     * block or standalone set with a fixed rarity budget — exactly two rares/mythics, exactly six
     * uncommons, any number of commons — plus the five basics. See [PrimordialFormat]. Scryfall
     * tracks no legality for it; the deck validator reads the pools off the set printings.
     */
    PRIMORDIAL;

    /**
     * Lower-case identifier matching Scryfall's `legalities.<key>` field. Most enum names map
     * directly (`STANDARD` → `"standard"`); `STANDARD_BRAWL` is special-cased because Scryfall
     * concatenates words. Null for the formats Scryfall doesn't track: the Highlander formats'
     * legality is derived from Vintage's at load time, Primordial's from the set printings.
     */
    val scryfallKey: String? get() = when (this) {
        STANDARD_BRAWL -> "standardbrawl"
        HIGHLANDER, CANADIAN_HIGHLANDER, PRIMORDIAL -> null
        else -> name.lowercase()
    }

    /** Human-readable label for UI surfaces. */
    val displayName: String get() = when (this) {
        STANDARD_BRAWL -> "Standard Brawl"
        CANADIAN_HIGHLANDER -> "Canadian Highlander"
        else -> name.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * True for the 100-card singleton formats played without a commander. They take the
     * constructed baseline's life total and table, and differ from it only in deck construction.
     */
    val isHighlanderShape: Boolean get() = this == HIGHLANDER || this == CANADIAN_HIGHLANDER

    /**
     * True for formats whose decks come from one set or block with a per-rarity budget (Primordial).
     * Per-card legality there depends on the deck's chosen pool, not on the card alone.
     */
    val isSetBased: Boolean get() = this == PRIMORDIAL

    /**
     * True for singleton formats with a designated commander (Commander, Brawl, Standard Brawl).
     * These formats add structural rules beyond the constructed baseline: a commander card lives
     * in the command zone, every other card's color identity must fit within the commander's,
     * and (for partner-less decks) the commander itself must be a legal commander.
     */
    val isCommanderShape: Boolean get() = when (this) {
        COMMANDER, BRAWL, STANDARD_BRAWL -> true
        else -> false
    }

    companion object {
        fun fromScryfallKey(key: String): DeckFormat? =
            entries.firstOrNull { it.scryfallKey == key.lowercase() }
    }
}
