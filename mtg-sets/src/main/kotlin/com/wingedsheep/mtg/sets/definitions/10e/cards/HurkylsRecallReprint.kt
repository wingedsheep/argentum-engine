package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hurkyl's Recall reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in ATQ's `cards/`
 * package (the card's earliest real printing). This file contributes only the 10E-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HurkylsRecallReprint = Printing(
    oracleId = "ed1e5d24-c8a8-48fe-a88f-1003ad432477",
    name = "Hurkyl's Recall",
    setCode = "10E",
    collectorNumber = "88",
    scryfallId = "a7a42c38-6129-4e6e-9e27-e2c812ce6f45",
    artist = "Ralph Horsley",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7a42c38-6129-4e6e-9e27-e2c812ce6f45.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
