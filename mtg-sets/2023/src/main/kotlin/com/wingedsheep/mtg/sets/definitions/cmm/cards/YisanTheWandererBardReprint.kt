package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Yisan, the Wanderer Bard reprints in CMM — the deck printing (#334) and the etched foil printing
 * (#577). Canonical CardDefinition lives in its earliest set (M15).
 */
val YisanTheWandererBardReprint = Printing(
    oracleId = "7f4b0233-a2b0-41a2-8448-2df1f845148c",
    name = "Yisan, the Wanderer Bard",
    setCode = "CMM",
    collectorNumber = "334",
    scryfallId = "94e3a130-deeb-4bf4-a1f6-9219c3d8c373",
    artist = "Chase Stone",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/94e3a130-deeb-4bf4-a1f6-9219c3d8c373.jpg?1783915616",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)

val YisanTheWandererBardEtchedReprint = Printing(
    oracleId = "7f4b0233-a2b0-41a2-8448-2df1f845148c",
    name = "Yisan, the Wanderer Bard",
    setCode = "CMM",
    collectorNumber = "577",
    scryfallId = "0a76f047-0143-4485-bb37-3a0159f4d5fa",
    artist = "Chase Stone",
    imageUri = "https://cards.scryfall.io/normal/front/0/a/0a76f047-0143-4485-bb37-3a0159f4d5fa.jpg?1783915533",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)
