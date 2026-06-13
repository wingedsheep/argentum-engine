package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * War Mammoth reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WarMammothReprint = Printing(
    oracleId = "b2a8fcf9-2ec2-40a9-9955-90f8605b11a3",
    name = "War Mammoth",
    setCode = "LEB",
    collectorNumber = "228",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/9/f/9f67175d-ac5c-4947-b243-d5206b552bdc.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
