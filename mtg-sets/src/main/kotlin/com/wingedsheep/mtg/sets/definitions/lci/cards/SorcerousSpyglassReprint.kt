package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sorcerous Spyglass reprint in The Lost Caverns of Ixalan (LCI).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in the Ixalan (XLN)
 * set's `cards/` package — XLN is the earliest real printing. This file contributes only the
 * LCI-specific presentation row (set, collector number, art), picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SorcerousSpyglassReprint = Printing(
    oracleId = "b2187f45-80ae-4ac4-9f83-5eb7a00978e2",
    name = "Sorcerous Spyglass",
    setCode = "LCI",
    collectorNumber = "261",
    scryfallId = "194b7899-6b44-4ecc-8ddc-ec24304eb14c",
    artist = "Tyler Walpole",
    imageUri = "https://cards.scryfall.io/normal/front/1/9/194b7899-6b44-4ecc-8ddc-ec24304eb14c.jpg?1782694401",
    releaseDate = "2023-11-17",
    rarity = Rarity.UNCOMMON,
)
