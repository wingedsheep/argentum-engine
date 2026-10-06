package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flames of the Firebrand reprint in JMP. Canonical CardDefinition lives in Magic 2013 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m13.cards.FlamesOfTheFirebrand`.
 */
val FlamesOfTheFirebrandReprint = Printing(
    oracleId = "bc32c24f-2f2a-4125-917c-60d425166640",
    name = "Flames of the Firebrand",
    setCode = "JMP",
    collectorNumber = "317",
    scryfallId = "584cdb52-08f8-425b-8407-8192b1dc6843",
    artist = "Steve Argyle",
    imageUri = "https://cards.scryfall.io/normal/front/5/8/584cdb52-08f8-425b-8407-8192b1dc6843.jpg?1783930395",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)
