package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Void Shatter
 * {1}{U}{U}
 * Instant
 * Devoid (This card has no color.)
 * Counter target spell. If that spell is countered this way, exile it instead of putting it into
 * its owner's graveyard.
 *
 * A devoid Dissipate: [Effects.CounterSpellToExile] on any spell.
 */
val VoidShatter = card("Void Shatter") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Counter target spell. If that spell is countered this way, exile it instead of putting it " +
        "into its owner's graveyard."

    keywords(Keyword.DEVOID)

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Jason Felix"
        flavorText = "Eldrazi break concentration like glass."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bf13c5e-3968-48ad-ba08-99ba58873223.jpg?1783937920"
    }
}
