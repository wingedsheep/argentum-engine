package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color

/**
 * One token printing a set contributes, so a token minted by that set's cards shows *that set's*
 * art instead of the engine-wide generic fallback.
 *
 * A token is not a card: it has no [CardDefinition] and no [Printing] row, so the per-printing
 * art override that [Printing] gives a real card has nothing to hang on. Sets therefore declare
 * their token art here, on [MtgSet.tokenArt], and the engine resolves it at creation time from
 * the printing of the card doing the creating. That keeps art out of the card script — a card
 * that hardcodes `imageUri` on its `CreateToken` effect mints the same art from every printing,
 * which is wrong the moment the card is reprinted in a set with its own token.
 *
 * ## Matching
 * [name] must match; [power], [toughness] and [colors] are optional discriminators that only
 * participate when non-null. Leave them out unless the set prints two different tokens sharing a
 * name — e.g. a white 1/1 Cat and a green 2/2 Cat — in which case spell out enough to separate
 * them. See [matches].
 *
 * ## Several arts for one token
 * A set that printed the *same* token with several illustrations declares one row per art —
 * Jumpstart's four Dog tokens are four `TokenPrinting(name = "Dog", …)` rows differing only in
 * [imageUri]. Nothing about the row changes; the plurality lives in the list. When a card mints a
 * batch of them the engine deals the arts out in order ([allMatches]), so Release the Dogs' four
 * Dogs show all four printed illustrations instead of one repeated four times.
 *
 * ## Image form
 * Use the Scryfall **`art_crop`** URL, not `normal`. The client renders a token as a generated
 * frame (name bar / art box / type bar) and drops this image into the art box, so a full-card
 * `normal` image arrives pre-framed and gets cropped to its middle band.
 *
 * @property name Token name as printed — the creature type for a vanilla token ("Cat"), or the
 *   full name for a named one ("Marit Lage", "Zombie Druid").
 * @property imageUri Scryfall `art_crop` URL for this set's printing of the token.
 * @property power Printed power, when needed to disambiguate. Null matches any.
 * @property toughness Printed toughness, when needed to disambiguate. Null matches any.
 * @property colors Printed colors, when needed to disambiguate. Null matches any; an empty set
 *   means *colorless* and only matches a colorless token.
 */
data class TokenPrinting(
    val name: String,
    val imageUri: String,
    val power: Int? = null,
    val toughness: Int? = null,
    val colors: Set<Color>? = null,
) {
    /**
     * Whether this printing describes the token being created. The name must match
     * ([matchesName]); each of [power], [toughness] and [colors] is checked only when this
     * printing pins it, so a bare `TokenPrinting("Cat", art)` matches every Cat the set mints.
     */
    fun matches(
        name: String,
        power: Int? = null,
        toughness: Int? = null,
        colors: Set<Color> = emptySet(),
    ): Boolean =
        matchesName(name) &&
            (this.power == null || this.power == power) &&
            (this.toughness == null || this.toughness == toughness) &&
            (this.colors == null || this.colors == colors)

    /**
     * Name match alone, ignoring [power] / [toughness] / [colors].
     *
     * Case-insensitive, and order-insensitive across words: a token minted from
     * `creatureTypes = setOf("Army", "Zombie")` is named by joining an unordered set, so it can
     * arrive as either "Zombie Army" or "Army Zombie" depending on iteration order, while the
     * printed name has one canonical order.
     */
    fun matchesName(name: String): Boolean =
        this.name.equals(name, ignoreCase = true) || words(this.name) == words(name)

    private fun words(value: String): Set<String> =
        value.split(' ').filter { it.isNotEmpty() }.mapTo(mutableSetOf()) { it.lowercase() }

    companion object {
        /**
         * Best row for a token among [printings], or null when the set prints nothing matching.
         *
         * Exact identity first, so a set printing two tokens that share a name (a 1/1 white
         * Soldier and a 2/2 white Soldier) resolves to the right one. Then name alone: synced rows
         * pin P/T and colors from the printed token and the engine's token can legitimately differ
         * (a variable-P/T printing, a colour granted rather than printed), and the set's own art for the
         * right token name still beats engine-wide generic art.
         *
         * Shared by `TokenArtRegistry` (runtime resolution) and the token-art gap report, so the
         * report can never disagree with what the engine will actually do.
         */
        fun bestMatch(
            printings: List<TokenPrinting>,
            name: String,
            power: Int? = null,
            toughness: Int? = null,
            colors: Set<Color> = emptySet(),
        ): TokenPrinting? = allMatches(printings, name, power, toughness, colors).firstOrNull()

        /**
         * Every printing describing this token, in declaration order — the set's whole run of arts
         * for it, which a batch of tokens created at once is dealt out of.
         *
         * Same two tiers as [bestMatch], applied wholesale: if any row pins this exact identity,
         * *those* are the arts and the looser name-only rows are ignored — a set printing a white
         * 1/1 Cat and a green 2/2 Cat must not cycle both arts for either one. Only when nothing
         * pins the identity does the name-only tier stand in, for the same reason [bestMatch]
         * falls back to it: a synced row pins P/T and colors from the printed token, and the
         * engine's token can legitimately differ.
         */
        fun allMatches(
            printings: List<TokenPrinting>,
            name: String,
            power: Int? = null,
            toughness: Int? = null,
            colors: Set<Color> = emptySet(),
        ): List<TokenPrinting> =
            printings.filter { it.matches(name, power, toughness, colors) }
                .ifEmpty { printings.filter { it.matchesName(name) } }
    }
}
