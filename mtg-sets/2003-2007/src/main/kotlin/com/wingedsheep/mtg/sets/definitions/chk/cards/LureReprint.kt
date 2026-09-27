package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in CHK.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val LureChkReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "CHK",
    collectorNumber = "226",
    scryfallId = "11c193e4-4484-46c1-83ce-c421d47bed9f",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/1/1/11c193e4-4484-46c1-83ce-c421d47bed9f.jpg?1783944286",
    releaseDate = "2004-10-01",
    rarity = Rarity.UNCOMMON,
)
