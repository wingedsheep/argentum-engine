package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sorcerous Spyglass reprint in Foundations (FDN). Canonical CardDefinition lives in XLN (the
 * earliest real printing); this file contributes only the FDN presentation row, picked up
 * automatically by `CardDiscovery.findPrintingsIn`.
 */
val SorcerousSpyglassReprint = Printing(
    oracleId = "b2187f45-80ae-4ac4-9f83-5eb7a00978e2",
    name = "Sorcerous Spyglass",
    setCode = "FDN",
    collectorNumber = "679",
    scryfallId = "0f99356b-eed0-4d92-818b-80754bcb75f3",
    artist = "Kieran Yanner",
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0f99356b-eed0-4d92-818b-80754bcb75f3.jpg?1783908903",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
