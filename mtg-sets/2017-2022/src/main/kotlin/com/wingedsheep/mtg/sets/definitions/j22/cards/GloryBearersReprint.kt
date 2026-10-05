package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Glory Bearers reprint in J22. Canonical CardDefinition lives in Theros Beyond Death (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.thb.cards.GloryBearers`.
 */
val GloryBearersReprint = Printing(
    oracleId = "8f446660-f72a-454f-bc96-3b6dd8633228",
    name = "Glory Bearers",
    setCode = "J22",
    collectorNumber = "191",
    scryfallId = "9a0f1eb3-379d-4dc0-a3c8-618cfd3414b2",
    artist = "Tyler Walpole",
    imageUri = "https://cards.scryfall.io/normal/front/9/a/9a0f1eb3-379d-4dc0-a3c8-618cfd3414b2.jpg?1783919111",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
