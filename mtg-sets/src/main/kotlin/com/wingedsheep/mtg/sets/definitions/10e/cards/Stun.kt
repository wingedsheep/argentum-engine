package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stun reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StunReprint = Printing(
    oracleId = "d3491972-44c5-4962-a680-42b79357a189",
    name = "Stun",
    setCode = "10E",
    collectorNumber = "240",
    artist = "Terese Nielsen",
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0f4e2c82-cdb8-4745-9af6-804717c541d7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
