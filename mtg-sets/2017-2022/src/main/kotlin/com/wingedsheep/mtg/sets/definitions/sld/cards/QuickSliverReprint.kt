package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Quick Sliver reprint in SLD. Canonical CardDefinition lives in its earliest set.
 */
val QuickSliverReprint = Printing(
    oracleId = "530a5740-041c-42df-8fe0-fcea87258038",
    name = "Quick Sliver",
    setCode = "SLD",
    collectorNumber = "655",
    scryfallId = "4702896b-7bd7-48fa-9bbb-69d50a31178e",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/4702896b-7bd7-48fa-9bbb-69d50a31178e.jpg",
    releaseDate = "2022-12-16",
    rarity = Rarity.RARE,
)
