package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dawnhart Disciple reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VOW's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DawnhartDiscipleReprint = Printing(
    oracleId = "e86279ba-cb22-4754-90f4-38354657b8fa",
    name = "Dawnhart Disciple",
    setCode = "INR",
    collectorNumber = "191",
    artist = "Mila Pesic",
    imageUri = "https://cards.scryfall.io/normal/front/4/4/4463b28a-c2c9-4cbf-951e-e3ce6b1f14cb.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
