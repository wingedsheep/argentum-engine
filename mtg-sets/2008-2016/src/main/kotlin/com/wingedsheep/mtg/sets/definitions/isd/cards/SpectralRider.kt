package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spectral Rider
 * {W}{W}
 * Creature — Spirit Knight
 * 2/2
 * Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)
 */
val SpectralRider = card("Spectral Rider") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Knight"
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)"
    power = 2
    toughness = 2
    keywords(Keyword.INTIMIDATE)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Igor Kieryluk"
        flavorText = "Never a word. Never a warning. Only the sound of hoofbeats along the crossways, and the screams of the avenged."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b47e4e56-8bde-480d-b59c-17a017665b19.jpg"
    }
}
