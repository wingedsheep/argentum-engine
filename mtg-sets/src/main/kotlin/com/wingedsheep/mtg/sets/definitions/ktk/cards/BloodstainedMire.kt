package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodstained Mire reprint in KTK.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes
 * only the KTK-specific presentation row.
 */
val BloodstainedMireReprint = Printing(
    oracleId = "fc0707c7-d504-4ccf-a0d2-3eb6e26e7a57",
    name = "Bloodstained Mire",
    setCode = "KTK",
    collectorNumber = "230",
    scryfallId = "7f430794-0d86-4f6a-97e0-4bbb6716d613",
    artist = "Daarken",
    imageUri = "https://cards.scryfall.io/normal/front/7/f/7f430794-0d86-4f6a-97e0-4bbb6716d613.jpg?1707235037",
    releaseDate = "2014-09-26",
    rarity = Rarity.RARE,
)
