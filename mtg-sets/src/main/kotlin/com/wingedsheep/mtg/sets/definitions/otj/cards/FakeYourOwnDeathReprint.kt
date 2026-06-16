package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fake Your Own Death reprint in Outlaws of Thunder Junction.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in the SNC `cards/`
 * package (Streets of New Capenna, the card's earliest real printing); this file
 * contributes only OTJ presentation data.
 */
val FakeYourOwnDeathReprint = Printing(
    oracleId = "ad01df89-29fe-44c7-a133-91425f8ff09c",
    name = "Fake Your Own Death",
    setCode = "OTJ",
    collectorNumber = "87",
    scryfallId = "79a17ab9-13c9-41d4-a143-82d8caacfd8b",
    artist = "Monztre",
    imageUri = "https://cards.scryfall.io/normal/front/7/9/79a17ab9-13c9-41d4-a143-82d8caacfd8b.jpg?1712355584",
    releaseDate = "2024-04-19",
    rarity = Rarity.COMMON,
)
