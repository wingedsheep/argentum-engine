package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stalwart Valkyrie reprint in J22. Canonical CardDefinition lives in Kaldheim (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.khm.cards.StalwartValkyrie`.
 */
val StalwartValkyrieReprint = Printing(
    oracleId = "b6b4c459-9c8b-4b95-8706-05d972b8daea",
    name = "Stalwart Valkyrie",
    setCode = "J22",
    collectorNumber = "249",
    scryfallId = "ebc7746a-ff2a-4bc1-bc30-45eb53a94ddc",
    artist = "Jason Rainville",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/ebc7746a-ff2a-4bc1-bc30-45eb53a94ddc.jpg?1783919084",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
