package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chandra's Spitfire reprint in M20. Canonical CardDefinition lives in Magic 2011 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m11.cards.ChandrasSpitfire`.
 */
val ChandrasSpitfireReprint = Printing(
    oracleId = "5a0eb270-b142-45da-87a2-2f1c4e25db17",
    name = "Chandra's Spitfire",
    setCode = "M20",
    collectorNumber = "132",
    scryfallId = "8a857da7-5438-465b-821a-bd5bfd780c69",
    artist = "Chris Rallis",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a857da7-5438-465b-821a-bd5bfd780c69.jpg?1783932982",
    releaseDate = "2019-07-12",
    rarity = Rarity.UNCOMMON,
)
