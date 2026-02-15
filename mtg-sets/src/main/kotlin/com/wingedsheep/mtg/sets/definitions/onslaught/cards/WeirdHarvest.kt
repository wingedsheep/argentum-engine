package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EachPlayerSearchesLibraryEffect

/**
 * Weird Harvest
 * {X}{G}{G}
 * Sorcery
 * Each player may search their library for up to X creature cards, reveal those cards,
 * put them into their hand, then shuffle.
 */
val WeirdHarvest = card("Weird Harvest") {
    manaCost = "{X}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Each player may search their library for up to X creature cards, reveal those cards, put them into their hand, then shuffle."

    spell {
        effect = EachPlayerSearchesLibraryEffect(
            filter = Filters.Creature,
            count = DynamicAmount.XValue
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "299"
        artist = "Bob Petillo"
        flavorText = "Krosa's distorted groves bear strange fruit."
        imageUri = "https://cards.scryfall.io/large/front/8/9/89bd2c25-1571-4f73-a12c-ec4e7f38e176.jpg?1562842301"
    }
}
