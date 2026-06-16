package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Balloon Brigade reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinBalloonBrigadeReprint = Printing(
    oracleId = "10bc98b0-3fdc-46d1-8d3b-6d160e9dd62f",
    name = "Goblin Balloon Brigade",
    setCode = "LEB",
    collectorNumber = "154",
    artist = "Andi Rusu",
    imageUri = "https://cards.scryfall.io/normal/front/3/f/3fdb52dd-4fc5-4594-b53b-ea169325be0b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
