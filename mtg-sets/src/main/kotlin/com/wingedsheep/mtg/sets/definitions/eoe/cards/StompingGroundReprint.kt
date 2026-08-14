package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stomping Ground reprint in Edge of Eternities. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val StompingGroundReprint = Printing(
    oracleId = "16052b52-ade1-406f-a06b-ce7ea607fb63",
    name = "Stomping Ground",
    setCode = "EOE",
    collectorNumber = "258",
    scryfallId = "69be21b4-c613-47c6-ba57-f4785861af3e",
    artist = "Bruce Brenneise",
    imageUri = "https://cards.scryfall.io/normal/front/6/9/69be21b4-c613-47c6-ba57-f4785861af3e.jpg?1752947608",
    releaseDate = "2025-08-01",
    rarity = Rarity.RARE,
)
