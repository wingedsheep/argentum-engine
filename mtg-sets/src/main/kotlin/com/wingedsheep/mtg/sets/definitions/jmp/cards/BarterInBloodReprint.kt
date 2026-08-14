package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Barter in Blood reprint in Jumpstart. Canonical CardDefinition lives in Mirrodin (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.BarterInBlood`.
 */
val BarterInBloodJmpReprint = Printing(
    oracleId = "9167998d-5cac-47d7-99f2-f38122f7b8e7",
    name = "Barter in Blood",
    setCode = "JMP",
    collectorNumber = "202",
    scryfallId = "23986add-b33d-4bad-86f3-e2d0f99cf949",
    artist = "Eric Deschamps",
    imageUri = "https://cards.scryfall.io/normal/front/2/3/23986add-b33d-4bad-86f3-e2d0f99cf949.jpg?1783930436",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)
