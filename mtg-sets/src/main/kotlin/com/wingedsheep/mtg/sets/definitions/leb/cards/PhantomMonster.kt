package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phantom Monster reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhantomMonsterReprint = Printing(
    oracleId = "bbd30183-524c-4b93-b953-90853ec3f39f",
    name = "Phantom Monster",
    setCode = "LEB",
    collectorNumber = "70",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0782e90-383b-4aed-8fa0-99c8cf8b2cec.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
