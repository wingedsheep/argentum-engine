package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chart a Course reprint in Bloomburrow Commander (BLC). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Ixalan's `cards/` package;
 * this file contributes only the BLC presentation row.
 */
val ChartACourseReprint = Printing(
    oracleId = "05878e49-93ad-4144-9c50-a0bb86126c2e",
    name = "Chart a Course",
    setCode = "BLC",
    collectorNumber = "110",
    scryfallId = "b005b866-b83c-4413-9db7-465f614a4029",
    artist = "Olena Richards",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b005b866-b83c-4413-9db7-465f614a4029.jpg?1782690209",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
