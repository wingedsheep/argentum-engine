package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterSpellEffect
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect

/**
 * Discombobulate
 * {2}{U}{U}
 * Instant
 * Counter target spell. Look at the top four cards of your library, then put them back in any order.
 */
val Discombobulate = card("Discombobulate") {
    manaCost = "{2}{U}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell. Look at the top four cards of your library, then put them back in any order."

    spell {
        target = Targets.Spell
        effect = CounterSpellEffect then LookAtTopAndReorderEffect(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef584c5-6e2d-419b-9c11-a1b6c9c9ab2a.jpg?1562943839"
    }
}
