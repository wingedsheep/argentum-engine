package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.sdk.core.Zone

/**
 * How a spell got onto the stack — which alternative cost paid for it, and which zone it came from.
 *
 * Nothing in the client view used to carry either fact, so every cast rendered identically to a
 * plain cast out of hand. That is worst for the graveyard casts that also change the card's face:
 * a disturb spell (CR 702.146a) arrives under a name the opponent has never seen, with no printed
 * mana cost of its own, and its graveyard card silently disappears — so "Opponent cast Ghostly
 * Castigator" reads as though the card materialised in their hand. Flashback, harmonize, mayhem and
 * commander casts have the same blind spot in milder form.
 *
 * An alternative cost also hides *how much* was actually paid, and emerge (CR 702.119a) hides *what
 * was eaten to make it cheaper*: a `{7}` Eldrazi resolving while its controller had four lands looks
 * like a bug from the other seat unless the sacrifice and the mana are both named. So the same
 * (method, origin) pair is joined here by two more facts — [sacrificeLabel] for the body an
 * alternative cost consumed, and [paidManaCost] for the mana that actually left the pool.
 *
 * Every rendering lives here so the naming table exists once: [logPhrase] for the game log line,
 * [badgeLabel] / [sacrificeLabel] / [paidManaCost] for the badges on the stack card. A plain cast
 * from hand has no provenance worth showing and every entry point returns null for it.
 */
object CastProvenance {

    /**
     * The game log's phrase, e.g. `"disturb, from graveyard"` or
     * `"emerge, sacrificed Niblis of the Urn, paid 4 mana"`, or null when the cast was an ordinary
     * one from hand. The caller parenthesises it, so the cast line still reads normally on its own.
     *
     * [sacrificedNames] and [manaSpent] only ever *extend* a phrase — a plain hand cast stays silent
     * even when it sacrificed something for an additional cost, because its printed cost is already
     * on the card and `PermanentsSacrificedEvent` already logs the sacrifice on its own line. It is
     * the alternative-cost casts, where neither number is visible anywhere, that need them.
     */
    fun logPhrase(
        alternativeCost: AlternativeCostType?,
        castFromZone: Zone?,
        sacrificedNames: List<String> = emptyList(),
        manaSpent: Int? = null,
    ): String? {
        val method = alternativeCost?.let(::methodName)
        val origin = castFromZone?.let(::originName)?.let { "from $it" }
        if (method == null && origin == null) return null
        val sacrifice = sacrificedPhrase(sacrificedNames)
        val paid = manaSpent?.takeIf { it > 0 }?.let { "paid $it mana" }
        return listOfNotNull(method, sacrifice, origin, paid).joinToString(", ")
    }

    /**
     * The stack card's badge, e.g. `"Disturb · Graveyard"`, or null for an ordinary cast from hand.
     * Rendered verbatim by the client — like `ClientCard.optionalCostLabel`, the wording stays
     * server-side so the client never maps enum names to words itself.
     */
    fun badgeLabel(alternativeCost: AlternativeCostType?, castFromZone: Zone?): String? {
        val method = alternativeCost?.let(::methodName)
        val origin = castFromZone?.let(::originName)
        val parts = listOfNotNull(method, origin).map { it.replaceFirstChar(Char::uppercase) }
        return parts.ifEmpty { null }?.joinToString(" · ")
    }

    /**
     * The stack card's badge for what an alternative cost consumed — `"Sacrificed Niblis of the
     * Urn"` — or null when it consumed nothing. Its own badge rather than a suffix on [badgeLabel]
     * so each row of the stack card carries exactly one fact.
     */
    fun sacrificeLabel(sacrificedNames: List<String>): String? =
        sacrificedPhrase(sacrificedNames)?.replaceFirstChar(Char::uppercase)

    /**
     * The mana that actually paid for this cast, as a mana-cost string the client renders as pips
     * (`"{W}{W}{W}{U}"`), or null when no mana was spent (a free cast, or one paid entirely with an
     * alternative payment). Colors are listed in WUBRG order followed by colorless, which is the
     * order costs are printed in; the *count* is what matters — this is what was spent, not a cost
     * to pay again, so generic mana appears as whatever colors happened to pay it.
     */
    fun paidManaCost(
        white: Int,
        blue: Int,
        black: Int,
        red: Int,
        green: Int,
        colorless: Int,
    ): String? {
        val pips = listOf("W" to white, "U" to blue, "B" to black, "R" to red, "G" to green, "C" to colorless)
        val rendered = pips.joinToString("") { (symbol, count) -> "{$symbol}".repeat(count.coerceAtLeast(0)) }
        return rendered.ifEmpty { null }
    }

    /** `"sacrificed Niblis of the Urn"` / `"sacrificed a Spirit, Grizzly Bears"`, or null for none. */
    private fun sacrificedPhrase(sacrificedNames: List<String>): String? =
        sacrificedNames.ifEmpty { null }?.let { "sacrificed ${it.joinToString(", ")}" }

    /**
     * The zone worth naming in a cast description, or null when it isn't. Hand is the assumed
     * default and would only add noise; the battlefield, stack and sideboard are never a cast's
     * origin zone (a wish moves the card to hand first), so they fall through to null too.
     */
    private fun originName(zone: Zone): String? = when (zone) {
        Zone.GRAVEYARD -> "graveyard"
        Zone.EXILE -> "exile"
        Zone.COMMAND -> "command zone"
        Zone.LIBRARY -> "library"
        Zone.HAND, Zone.BATTLEFIELD, Zone.STACK, Zone.SIDEBOARD -> null
    }

    /**
     * The player-facing name of an alternative casting cost. Exhaustive on purpose: a new
     * [AlternativeCostType] must decide how it reads to the opponent rather than silently
     * inheriting a generic label.
     */
    private fun methodName(type: AlternativeCostType): String = when (type) {
        AlternativeCostType.FLASHBACK -> "flashback"
        AlternativeCostType.HARMONIZE -> "harmonize"
        AlternativeCostType.MAYHEM -> "mayhem"
        AlternativeCostType.DISTURB -> "disturb"
        AlternativeCostType.WARP -> "warp"
        AlternativeCostType.DASH -> "dash"
        AlternativeCostType.EVOKE -> "evoke"
        AlternativeCostType.EMERGE -> "emerge"
        AlternativeCostType.SNEAK -> "sneak"
        AlternativeCostType.WEB_SLINGING -> "web-slinging"
        AlternativeCostType.IMPENDING -> "impending"
        AlternativeCostType.CLEAVE -> "cleave"
        AlternativeCostType.MIRACLE -> "miracle"
        // Not an alternative cost the opponent could look up as a keyword — CR 712.11b calls it
        // choosing which face you are casting, and the cost paid is that face's own mana cost.
        // Phrased as a bare noun like its neighbours, because `badgeLabel` capitalizes the first
        // character: "Back face", not "Its back face".
        AlternativeCostType.MODAL_BACK_FACE -> "back face"
        // Neither of these names a printed keyword the opponent could look up, so they read as the
        // generic fact: this spell was not paid for with its mana cost.
        AlternativeCostType.SELF_ALTERNATIVE, AlternativeCostType.GRANTED -> "alternative cost"
    }
}
