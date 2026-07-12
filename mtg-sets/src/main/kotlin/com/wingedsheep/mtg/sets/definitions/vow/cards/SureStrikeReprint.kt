package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sure Strike reprint in VOW.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in BFZ's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * VOW-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SureStrikeReprint = Printing(
    oracleId = "fb694e7e-f66e-4958-b6ed-aa74bc9ac43e",
    name = "Sure Strike",
    setCode = "VOW",
    collectorNumber = "179",
    scryfallId = "1d872736-fafb-44e8-a809-48c5436c665a",
    artist = "Lie Setiawan",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d872736-fafb-44e8-a809-48c5436c665a.jpg?1782703064",
    releaseDate = "2021-11-19",
    rarity = Rarity.COMMON,
)
