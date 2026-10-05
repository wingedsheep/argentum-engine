package com.wingedsheep.search

/**
 * Card projection consumed by the search engine.
 *
 * The search module never touches engine state — it operates on a flat
 * snapshot equivalent to the deckbuilder's client-side `CardSummary`. Any
 * consumer can implement this interface; `game-server`'s `CardSummaryDTO`
 * does so directly so we don't pay an extra mapping step on the hot path.
 *
 * Field semantics mirror the frontend's `CardSummary`:
 *   - `colors` are the printed mana-cost colors (`{R}` cost ⇒ `RED`).
 *   - `colorIdentity` is CR 903.4 — used by the `c:` / `id:` filters.
 *   - `cardTypes` / `supertypes` / `subtypes` are uppercase enum names.
 *   - `legalFormats` carries uppercase format names (STANDARD, MODERN, …).
 */
interface SearchCard {
    val name: String
    val manaCost: String
    val cmc: Int
    val colors: List<String>
    val colorIdentity: List<String>
    val cardTypes: List<String>
    val supertypes: List<String>
    val subtypes: List<String>
    val basicLand: Boolean
    val rarity: String
    val setCode: String?
    val collectorNumber: String?
    val oracleText: String?
    val power: String?
    val toughness: String?
    val keywords: List<String>
    val legalFormats: List<String>
    val isDoubleFaced: Boolean
    /**
     * Every set this card has a printing in — the canonical [setCode] plus every reprint
     * registered in `PrintingRegistry`. Used by the `s:`/`set:` matcher so a query like
     * `s:EOE Banishing Light` matches the card via its EOE reprint even though its
     * canonical [setCode] is BLB.
     *
     * Defaults to empty so existing implementers aren't forced to populate it; the
     * matcher always falls back to [setCode] for backwards compatibility.
     */
    val printingSetCodes: List<String> get() = emptyList()

    /**
     * Names printed on this card's printings when they differ from the oracle [name] — e.g.
     * Through the Omenpaths' "Ademi of the Silkchutes" for Spectacular Spider-Man. The name
     * matcher (bareword, `name:`, `!exact`) checks these alongside [name], as Scryfall does.
     */
    val printedNames: List<String> get() = emptyList()
}
