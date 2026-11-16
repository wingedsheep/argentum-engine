package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rending Volley reprint in TLE. The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * Dragons of Tarkir (`dtk`), the card's earliest real printing; this file contributes only
 * per-printing presentation data.
 */
val RendingVolleyReprint = Printing(
    oracleId = "0324941f-484c-4656-8d1c-66e919c2bc7c",
    name = "Rending Volley",
    setCode = "TLE",
    collectorNumber = "34",
    scryfallId = "26f073a7-e4a7-4b2a-a768-f649fe337f2d",
    artist = "Viacom",
    imageUri = "https://cards.scryfall.io/normal/front/2/6/26f073a7-e4a7-4b2a-a768-f649fe337f2d.jpg?1783904851",
    releaseDate = "2025-11-21",
    rarity = Rarity.MYTHIC,
    frameEffects = listOf("inverted"),
    isFullArt = true,
    borderColor = "borderless",
)
