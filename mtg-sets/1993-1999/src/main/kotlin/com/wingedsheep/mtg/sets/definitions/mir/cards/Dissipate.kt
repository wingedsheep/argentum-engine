package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dissipate
 * {1}{U}{U}
 * Instant
 * Counter target spell. If that spell is countered this way, exile it instead
 * of putting it into its owner's graveyard.
 */
val Dissipate = card("Dissipate") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell is countered this way, exile it instead of putting it into its owner's graveyard."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Richard Kane Ferguson"
        flavorText = "\"If you weren't born with it, you don't need it.\"\n—Grahilah, former trader of Amiqat"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36d9271d-6dbf-4640-9222-721a7a3ccc08.jpg?1783947110"
    }
}
