package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Squirrel Mob reprint in Bloomburrow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Odyssey (`ody`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val SquirrelMobReprint = Printing(
    oracleId = "451486df-6160-4ff7-b47c-dd3760417a31",
    name = "Squirrel Mob",
    setCode = "BLC",
    collectorNumber = "85",
    scryfallId = "7db8f11d-19c4-4b5b-8cb8-2ca86534ad2c",
    artist = "Justin Gerard",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7db8f11d-19c4-4b5b-8cb8-2ca86534ad2c.jpg?1783910710",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
    frameEffects = listOf("inverted"),
    borderColor = "borderless",
)
