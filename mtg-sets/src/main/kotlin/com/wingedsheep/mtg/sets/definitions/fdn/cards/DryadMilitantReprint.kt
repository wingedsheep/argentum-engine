package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dryad Militant reprint in FDN. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Return to Ravnica's `cards/` package;
 * this file contributes only the FDN presentation row.
 */
val DryadMilitantReprint = Printing(
    oracleId = "b8ca5877-ac9e-4b15-8c23-c70f61b01895",
    name = "Dryad Militant",
    setCode = "FDN",
    collectorNumber = "656",
    scryfallId = "8fb36712-26e9-4ec7-8946-626bb7094c15",
    artist = "Aaron J. Riley",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8fb36712-26e9-4ec7-8946-626bb7094c15.jpg?1782688696",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
