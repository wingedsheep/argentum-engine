package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deny Existence
 * {2}{U}
 * Instant
 * Counter target creature spell. If that spell is countered this way, exile it instead of putting
 * it into its owner's graveyard.
 *
 * Dissipate's [Effects.CounterSpellToExile], narrowed to creature spells on the stack (Deny the
 * Divine's shape with a creature-only target).
 */
val DenyExistence = card("Deny Existence") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature spell. If that spell is countered this way, exile it instead " +
        "of putting it into its owner's graveyard."

    spell {
        target(TargetFilter.CreatureSpellOnStack)
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Christopher Moeller"
        flavorText = "When all logical solutions fail, try an illogical one."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16a14eeb-1c85-4029-a047-39a4efef3f74.jpg?1783937802"
    }
}
