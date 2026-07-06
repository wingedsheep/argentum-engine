package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thrashing Brontodon reprint in LCI.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * RIX's `cards/` package (the card's earliest real printing). This file contributes only
 * the LCI-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThrashingBrontodonReprint = Printing(
    oracleId = "60bc63dc-ac9f-4a2f-aef5-c90d0aa31553",
    name = "Thrashing Brontodon",
    setCode = "LCI",
    collectorNumber = "216",
    artist = "Randy Vargas",
    imageUri = "https://cards.scryfall.io/normal/front/d/5/d52ef7c1-dacb-4204-b64e-5fa3ae3b1ace.jpg?1782694436",
    releaseDate = "2023-11-17",
    rarity = Rarity.UNCOMMON,
)
