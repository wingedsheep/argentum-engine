package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Blast reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LightningBlastReprint = Printing(
    oracleId = "91fd731c-e076-4f2d-9f22-872880c3cc3d",
    name = "Lightning Blast",
    setCode = "8ED",
    collectorNumber = "200",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/a/2/a20fee0e-93b8-4dc0-b156-8237271ae66c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
