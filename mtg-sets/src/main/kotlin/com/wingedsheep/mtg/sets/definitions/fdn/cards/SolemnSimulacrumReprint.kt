package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Solemn Simulacrum reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SolemnSimulacrumReprint = Printing(
    oracleId = "00c0543c-2a1f-4425-8283-4062d74a1637",
    name = "Solemn Simulacrum",
    setCode = "FDN",
    collectorNumber = "257",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/5/3/5383f45e-3da2-40fb-beee-801448bbb60f.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.RARE,
)
