package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect

/**
 * Scouting Trek
 * {1}{G}
 * Sorcery
 *
 * Search your library for any number of basic land cards, reveal those cards,
 * then shuffle and put them on top.
 */
val ScoutingTrek = card("Scouting Trek") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for any number of basic land cards, reveal those cards, then shuffle and put them on top."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(CardSource.FromZone(Zone.LIBRARY, filter = Filters.BasicLand), search = true)
            val found = chooseAnyNumber(
                from = searchable,
                prompt = "Search your library for any number of basic land cards"
            )
            reveal(found)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            toLibraryTop(found, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Stephanie Law"
        flavorText = "\"I have chosen my path. Who will walk it with me?\"\n—Eladamri"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b882e68-5c03-4ec6-9982-8c3b09847969.jpg?1562900439"
    }
}
