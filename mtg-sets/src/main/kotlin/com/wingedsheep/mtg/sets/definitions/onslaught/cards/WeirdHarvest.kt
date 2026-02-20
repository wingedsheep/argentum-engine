package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
        effect = EffectPatterns.eachPlayerSearchesLibrary(
            filter = Filters.Creature,
            count = DynamicAmount.XValue
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "299"
        artist = "Bob Petillo"
        flavorText = "Krosa's distorted groves bear strange fruit."
        imageUri = "https://cards.scryfall.io/large/front/3/c/3cdfa8b3-393b-4bb6-9265-faa4ab7126d2.jpg?1562909318"
    }
}
