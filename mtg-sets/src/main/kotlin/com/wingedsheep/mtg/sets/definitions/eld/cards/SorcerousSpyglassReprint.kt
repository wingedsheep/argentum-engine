package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sorcerous Spyglass reprint in Throne of Eldraine (ELD).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in the Ixalan (XLN)
 * set's `cards/` package — XLN is the earliest real printing. This file contributes only the
 * ELD-specific presentation row (set, collector number, art), picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SorcerousSpyglassReprint = Printing(
    oracleId = "b2187f45-80ae-4ac4-9f83-5eb7a00978e2",
    name = "Sorcerous Spyglass",
    setCode = "ELD",
    collectorNumber = "233",
    scryfallId = "e47e85d1-8c4a-43a9-92b3-7cb2a5b89219",
    artist = "Aaron Miller",
    imageUri = "https://cards.scryfall.io/normal/front/e/4/e47e85d1-8c4a-43a9-92b3-7cb2a5b89219.jpg?1783932581",
    releaseDate = "2019-10-04",
    rarity = Rarity.RARE,
)
