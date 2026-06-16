package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fake Your Own Death reprint in Foundations.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in the SNC `cards/`
 * package (Streets of New Capenna, the card's earliest real printing); this file
 * contributes only Foundations presentation data.
 */
val FakeYourOwnDeathReprint = Printing(
    oracleId = "ad01df89-29fe-44c7-a133-91425f8ff09c",
    name = "Fake Your Own Death",
    setCode = "FDN",
    collectorNumber = "174",
    scryfallId = "693635a6-df50-44c5-9598-0c79b45d4df4",
    artist = "Monztre",
    imageUri = "https://cards.scryfall.io/normal/front/6/9/693635a6-df50-44c5-9598-0c79b45d4df4.jpg?1730489248",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
