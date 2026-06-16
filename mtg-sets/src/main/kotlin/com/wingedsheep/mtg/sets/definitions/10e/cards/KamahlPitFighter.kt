package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kamahl, Pit Fighter reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val KamahlPitFighterReprint = Printing(
    oracleId = "153bb46f-ee8a-4da3-aab4-ae884f6ff403",
    name = "Kamahl, Pit Fighter",
    setCode = "10E",
    collectorNumber = "214",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/e/c/ec7b9336-5aa3-4992-b04b-4786150c6074.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
