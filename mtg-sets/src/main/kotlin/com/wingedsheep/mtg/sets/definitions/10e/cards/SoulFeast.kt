package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Feast reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SoulFeastReprint = Printing(
    oracleId = "8186fd80-015f-470c-9e1c-cbf45764a057",
    name = "Soul Feast",
    setCode = "10E",
    collectorNumber = "179",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5d0a8d6d-8cd0-4dfe-9894-77eb60620884.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
