package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ancestral Anger reprint in Secrets of Strixhaven.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in VOW's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * SOS-specific presentation row, picked up automatically by `CardDiscovery.findPrintingsIn`.
 */
val AncestralAngerReprint = Printing(
    oracleId = "e0828e8d-f01f-4088-9123-6d923ddb3242",
    name = "Ancestral Anger",
    setCode = "SOS",
    collectorNumber = "106",
    scryfallId = "6c5a93d6-d4ab-4062-bb3c-1b5330bf15ad",
    artist = "Gonzalo Kenny",
    imageUri = "https://cards.scryfall.io/normal/front/6/c/6c5a93d6-d4ab-4062-bb3c-1b5330bf15ad.jpg?1775937666",
    releaseDate = "2026-04-24",
    rarity = Rarity.COMMON,
)
