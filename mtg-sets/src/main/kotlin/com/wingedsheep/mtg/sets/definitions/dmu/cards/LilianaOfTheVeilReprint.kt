package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Liliana of the Veil reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, loyalty) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the DMU-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LilianaOfTheVeilReprint = Printing(
    oracleId = "0ba134d8-ee7d-48ec-8dc6-57942b8e9261",
    name = "Liliana of the Veil",
    setCode = "DMU",
    collectorNumber = "97",
    artist = "Martina Fačková",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d12c8c97-6491-452c-811d-943441a7ef9f.jpg?1783921329",
    releaseDate = "2022-09-09",
    rarity = Rarity.MYTHIC,
)
