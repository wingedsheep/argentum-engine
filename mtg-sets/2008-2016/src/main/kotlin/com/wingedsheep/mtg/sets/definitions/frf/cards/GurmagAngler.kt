package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gurmag Angler
 * {6}{B}
 * Creature — Zombie Fish
 * 5/5
 * Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)
 */
val GurmagAngler = card("Gurmag Angler") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Fish"
    power = 5
    toughness = 5
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)"

    keywords(Keyword.DELVE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "YW Tang"
        flavorText = "If everything in the Gurmag Swamp hungers for human flesh, what bait could be more effective?"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c60a8cf1-a8c7-4f45-bbd3-188fab2652f9.jpg"
    }
}
