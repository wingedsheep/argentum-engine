package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gruul Signet reprint in Bloomburrow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact set's `cards/`
 * package (Guildpact is the card's earliest real printing); this file contributes
 * only presentation data for the Bloomburrow Commander printing.
 */
val GruulSignetReprint = Printing(
    oracleId = "d36e0c9f-c025-4dfe-9644-9cad2461ce38",
    name = "Gruul Signet",
    setCode = "BLC",
    collectorNumber = "273",
    scryfallId = "1e71ad66-28ae-4cd6-ac71-d8710e9ea9cd",
    artist = "Efrem Palacios",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1e71ad66-28ae-4cd6-ac71-d8710e9ea9cd.jpg?1721975455",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
