package com.wingedsheep.mtg.sets.definitions.ddq.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Barter in Blood reprint in Duel Decks: Blessed vs. Cursed. Canonical CardDefinition lives in
 * Mirrodin (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.mrd.cards.BarterInBlood`.
 */
val BarterInBloodDdqReprint = Printing(
    oracleId = "9167998d-5cac-47d7-99f2-f38122f7b8e7",
    name = "Barter in Blood",
    setCode = "DDQ",
    collectorNumber = "52",
    scryfallId = "cb3411cb-8c43-4f06-8583-49dd9ba5db63",
    artist = "Eric Deschamps",
    imageUri = "https://cards.scryfall.io/normal/front/c/b/cb3411cb-8c43-4f06-8583-49dd9ba5db63.jpg?1783937842",
    releaseDate = "2016-02-26",
    rarity = Rarity.UNCOMMON,
)
