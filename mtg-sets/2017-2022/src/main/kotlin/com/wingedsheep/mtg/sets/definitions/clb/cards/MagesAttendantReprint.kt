package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mage's Attendant reprint in CLB. The canonical CardDefinition lives in
 * Streets of New Capenna (`snc`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val MagesAttendantReprint = Printing(
    oracleId = "94b1e7ed-87f3-44e6-bf34-186dd9a1a0d1",
    name = "Mage's Attendant",
    setCode = "CLB",
    collectorNumber = "698",
    scryfallId = "e7039d47-276b-4c7c-acc3-94a9baae97e5",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/e/7/e7039d47-276b-4c7c-acc3-94a9baae97e5.jpg?1783922487",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
