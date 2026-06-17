package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Grand Abolisher reprint in The Big Score. Canonical CardDefinition lives in
 * [com.wingedsheep.mtg.sets.definitions.m12.cards.GrandAbolisher] (its earliest real printing,
 * Magic 2012); this file contributes only presentation data.
 */
val GrandAbolisherReprint = Printing(
    oracleId = "c749f23c-40c0-4159-b84c-a70cbb062c14",
    name = "Grand Abolisher",
    setCode = "BIG",
    collectorNumber = "2",
    scryfallId = "ee793ed2-7d59-4640-8868-ad486600df2c",
    artist = "Aurore Folny",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee793ed2-7d59-4640-8868-ad486600df2c.jpg?1770090372",
    releaseDate = "2024-04-19",
    rarity = Rarity.MYTHIC,
)
