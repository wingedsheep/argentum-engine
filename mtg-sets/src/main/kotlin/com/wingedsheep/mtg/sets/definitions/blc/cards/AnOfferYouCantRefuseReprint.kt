package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * An Offer You Can't Refuse reprint in Bloomburrow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in SNC's `cards/` package (the card's
 * earliest real printing); this file contributes only the BLC presentation row.
 */
val AnOfferYouCantRefuseReprint = Printing(
    oracleId = "234a734b-ba28-4f1b-9d01-3c3e7d516590",
    name = "An Offer You Can't Refuse",
    setCode = "BLC",
    collectorNumber = "170",
    scryfallId = "b9ee82ee-4899-496e-80a6-e3403f6b364f",
    artist = "Dallas Williams",
    imageUri = "https://cards.scryfall.io/normal/front/b/9/b9ee82ee-4899-496e-80a6-e3403f6b364f.jpg?1782690159",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
