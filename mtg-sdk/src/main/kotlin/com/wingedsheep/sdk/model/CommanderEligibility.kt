package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Supertype

/**
 * Determines whether a card is a legal commander under CR 903.3 (Commander) and the analogous
 * Brawl/Standard Brawl rules.
 *
 * Default rule: a commander must be a legendary creature. The override clauses allow specific
 * non-creature cards (planeswalkers, the occasional artifact) to be commanders when their
 * oracle text says so explicitly — e.g. Daretti, Scrap Savant: "Daretti, Scrap Savant can be
 * your commander." Cards like Faceless One similarly sidestep the legendary-creature default.
 *
 * Partner / Background / Friends Forever pairs are deliberately out of scope here — the
 * surrounding deck model only carries a single commander. When pair support is added,
 * eligibility will need to consider both commanders together.
 *
 * It lives in the SDK rather than next to the deck validator because two modules ask the same
 * question of the same pure card data: `game-server` validates a submitted commander, and the
 * `ai` module's commander deck builder has to *pick* one. A second copy of the rule is exactly
 * how a deck the builder produces becomes a deck the validator rejects.
 */
object CommanderEligibility {

    private val CAN_BE_COMMANDER = Regex(
        """can be your commander""",
        RegexOption.IGNORE_CASE,
    )

    /** True if [card] satisfies the eligibility rule for a single (non-partnered) commander. */
    fun isLegalCommander(card: CardDefinition): Boolean {
        if (Supertype.LEGENDARY in card.typeLine.supertypes && card.typeLine.isCreature) return true
        // Explicit override clause — covers planeswalker commanders (Daretti, Freyalise, etc.)
        // and oddities like Faceless One. The clause is matched case-insensitively against the
        // card's own oracle text; we don't require it to name the card by string match because
        // some printings phrase it as "This card can be your commander."
        return CAN_BE_COMMANDER.containsMatchIn(card.oracleText)
    }
}
