package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * An Offer You Can't Refuse reprint in Foundations. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in SNC's `cards/` package (the card's
 * earliest real printing); this file contributes only the FDN presentation row.
 */
val AnOfferYouCantRefuseReprint = Printing(
    oracleId = "234a734b-ba28-4f1b-9d01-3c3e7d516590",
    name = "An Offer You Can't Refuse",
    setCode = "FDN",
    collectorNumber = "160",
    scryfallId = "a829747f-cf9b-4d81-ba66-9f0630ed4565",
    artist = "Dallas Williams",
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a829747f-cf9b-4d81-ba66-9f0630ed4565.jpg?1782689128",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
