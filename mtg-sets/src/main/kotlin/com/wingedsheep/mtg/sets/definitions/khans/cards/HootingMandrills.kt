package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hooting Mandrills
 * {5}{G}
 * Creature — Ape
 * 4/4
 * Delve
 * Trample
 */
val HootingMandrills = card("Hooting Mandrills") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Ape"
    power = 4
    toughness = 4

    keywords(Keyword.DELVE, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Mike Bierek"
        flavorText = "Interlopers in Sultai territory usually end up as crocodile chow or baboon bait."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/090d678c-f0e4-4757-8900-93dfe67aefe9.jpg?1562782269"
    }
}
