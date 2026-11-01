package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Open the Gates — Ravnica Allegiance #133
 * {G} · Sorcery
 *
 * Circuitous Route's basic-land-or-Gate filter on a single revealed tutor to hand.
 */
val OpenTheGates = card("Open the Gates") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card or Gate card, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Gate"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "John Thacker"
        flavorText = "\"If you don't have a destination in mind, the route is all that matters.\"\n—Tamina, district guide"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff326c57-accb-4b75-9bc9-b5ef1a85f38d.jpg"
    }
}
