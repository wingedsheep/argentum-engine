package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Weight of Memory
 * {3}{U}{U}
 * Sorcery
 * Draw three cards. Target player mills three cards.
 */
val WeightOfMemory = card("Weight of Memory") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Target player mills three cards."

    spell {
        val t = target("target", Targets.Player)
        effect = Effects.DrawCards(3)
            .then(LibraryPatterns.mill(3, t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Eric Deschamps"
        flavorText = "\"In lives that have stretched for centuries, there are bound to be a few awkward silences.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f2359aa-9230-4519-b12e-ec395a8dd2a0.jpg?1562733453"
    }
}
