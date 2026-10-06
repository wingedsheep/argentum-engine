package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soulcage Fiend reprint in J22. Canonical CardDefinition lives in Avacyn Restored (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.avr.cards.SoulcageFiend`.
 */
val SoulcageFiendReprint = Printing(
    oracleId = "ecbb660a-4013-410a-b683-82c3ef1690cd",
    name = "Soulcage Fiend",
    setCode = "J22",
    collectorNumber = "470",
    scryfallId = "c6f1d112-4c62-41c0-8be6-da78c1036b4d",
    artist = "Jason A. Engle",
    imageUri = "https://cards.scryfall.io/normal/front/c/6/c6f1d112-4c62-41c0-8be6-da78c1036b4d.jpg?1783918978",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
