package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shantotto, Tactician Magician — borderless variant (FIN #507).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] is [ShantottoTacticianMagician] (FIN #241,
 * same set); this row contributes only the alternate-art presentation data for the borderless printing.
 */
val ShantottoTacticianMagicianBorderless = Printing(
    oracleId = "381df197-5fb7-44ab-a31f-96c4993a0e53",
    name = "Shantotto, Tactician Magician",
    setCode = "FIN",
    collectorNumber = "507",
    scryfallId = "d1e01457-1696-4313-b21e-36e088735b1f",
    artist = "Joshua Raphael",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d1e01457-1696-4313-b21e-36e088735b1f.jpg?1748707561",
    releaseDate = "2025-06-13",
    rarity = Rarity.UNCOMMON,
)
