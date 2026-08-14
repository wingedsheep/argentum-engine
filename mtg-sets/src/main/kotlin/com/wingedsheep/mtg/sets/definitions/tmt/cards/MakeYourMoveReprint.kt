package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Make Your Move reprint in TMT.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in the card's earliest real
 * printing, Murders at Karlov Manor (`definitions/mkm/cards/MakeYourMove.kt`); this file
 * contributes only the TMT presentation row — set, collector number, art.
 */
val MakeYourMoveReprint = Printing(
    oracleId = "8226f31d-6f51-49c3-87f7-0c68f7f4f9ce",
    name = "Make Your Move",
    setCode = "TMT",
    collectorNumber = "20",
    scryfallId = "ed8bdd98-6377-40cf-b381-cee38b1bda2a",
    artist = "Nathaniel Himawan",
    imageUri = "https://cards.scryfall.io/normal/front/e/d/ed8bdd98-6377-40cf-b381-cee38b1bda2a.jpg?1783904163",
    releaseDate = "2026-03-06",
    rarity = Rarity.COMMON,
)
