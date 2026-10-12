package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Go for the Throat reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in MBS's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val GoForTheThroatReprint = Printing(
    oracleId = "2f092562-9e17-43cd-aeb8-d0567f99363e",
    name = "Go for the Throat",
    setCode = "BRO",
    collectorNumber = "102",
    scryfallId = "94117b09-d852-4f2e-be8e-68ee0ee3c3d8",
    artist = "Kristina Carroll",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/94117b09-d852-4f2e-be8e-68ee0ee3c3d8.jpg?1783920086",
    releaseDate = "2022-11-18",
    rarity = Rarity.UNCOMMON,
)
