package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fellwar Stone reprint in Bloomburrow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in `definitions/drk/cards/FellwarStone.kt` —
 * The Dark (1994-08-01) is the card's earliest printing — so this file contributes only
 * presentation data.
 */
val FellwarStoneReprint = Printing(
    oracleId = "95560508-7ac9-4be9-8a3f-3c7d5b52807b",
    name = "Fellwar Stone",
    setCode = "BLC",
    collectorNumber = "269",
    scryfallId = "e99c4fec-eb21-4288-a12f-1c58c4946bae",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e99c4fec-eb21-4288-a12f-1c58c4946bae.jpg?1721429553",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
