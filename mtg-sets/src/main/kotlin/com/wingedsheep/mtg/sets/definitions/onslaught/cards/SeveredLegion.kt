package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Severed Legion
 * {1}{B}{B}
 * Creature — Zombie
 * 2/2
 * Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)
 */
val SeveredLegion = card("Severed Legion") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2

    keywords(Keyword.FEAR)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Dany Orizio"
        flavorText = "No one in Aphetto answers a knock at the door after sundown."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efe12afd-da41-436e-af84-fa3b36a58030.jpg?1562951988"
    }
}
