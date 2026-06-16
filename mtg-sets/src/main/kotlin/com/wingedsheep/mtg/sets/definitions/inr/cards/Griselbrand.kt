package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Griselbrand reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GriselbrandReprint = Printing(
    oracleId = "f759d112-76db-4091-a22b-b9f19ab6fa5f",
    name = "Griselbrand",
    setCode = "INR",
    collectorNumber = "115",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/4069e510-f3f3-4668-9f13-3546fa9bc7c3.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)
