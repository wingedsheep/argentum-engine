package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cremate reprint in Guildpact. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Invasion (INV) set's `cards/` package — Cremate's earliest real printing —
 * so this file contributes only Guildpact presentation data.
 */
val CremateReprint = Printing(
    oracleId = "c6a2e410-b182-48d1-aeb2-bc8de27e9cd2",
    name = "Cremate",
    setCode = "GPT",
    collectorNumber = "45",
    scryfallId = "39a9ce8f-df8a-44bc-8d34-9f42135a3a23",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/3/9/39a9ce8f-df8a-44bc-8d34-9f42135a3a23.jpg?1593272158",
    releaseDate = "2006-02-03",
    rarity = Rarity.COMMON,
)
