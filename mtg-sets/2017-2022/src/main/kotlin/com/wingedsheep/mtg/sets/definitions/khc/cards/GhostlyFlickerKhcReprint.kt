package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghostly Flicker reprint in KHC. Canonical CardDefinition lives in Avacyn Restored (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.GhostlyFlicker`.
 */
val GhostlyFlickerKhcReprint = Printing(
    oracleId = "ad0070db-6454-41cb-861f-8f5b8fc2a3b8",
    name = "Ghostly Flicker",
    setCode = "KHC",
    collectorNumber = "39",
    scryfallId = "485ab561-9c2a-4f99-9317-8726bcdae364",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/4/8/485ab561-9c2a-4f99-9317-8726bcdae364.jpg?1783928324",
    releaseDate = "2021-02-05",
    rarity = Rarity.COMMON,
)
