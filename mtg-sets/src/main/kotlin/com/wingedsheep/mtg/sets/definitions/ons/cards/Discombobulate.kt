package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Discombobulate
 * {2}{U}{U}
 * Instant
 * Counter target spell. Look at the top four cards of your library, then put them back in any order.
 */
val Discombobulate = card("Discombobulate") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Look at the top four cards of your library, then put them back in any order."

    spell {
        target = Targets.Spell
        effect = CounterEffect() then LibraryPatterns.lookAtTopAndReorder(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef584c5-6e2d-419b-9c11-a1b6c9c9ab2a.jpg?1562943839"
    }
}
