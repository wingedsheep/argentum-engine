package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Interpret the Signs reprint in CMR. Canonical CardDefinition lives in Journey into Nyx (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.jou.cards.InterpretTheSigns`.
 */
val InterpretTheSignsReprint = Printing(
    oracleId = "834554f6-3b65-44fd-b1d1-8495b740dbfc",
    name = "Interpret the Signs",
    setCode = "CMR",
    collectorNumber = "75",
    scryfallId = "42dbf049-5c02-4038-86de-fa6b5110cf7e",
    artist = "Cynthia Sheppard",
    imageUri = "https://cards.scryfall.io/normal/front/4/2/42dbf049-5c02-4038-86de-fa6b5110cf7e.jpg?1783928861",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
