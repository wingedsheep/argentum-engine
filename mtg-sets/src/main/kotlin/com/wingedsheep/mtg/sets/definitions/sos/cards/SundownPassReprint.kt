package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sundown Pass reprint in Secrets of Strixhaven. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the VOW (Innistrad: Crimson Vow)
 * `cards/` package; this file contributes only presentation data.
 */
val SundownPassReprint = Printing(
    oracleId = "5ad0b405-cca4-475e-985c-4d7e3599d87e",
    name = "Sundown Pass",
    setCode = "SOS",
    collectorNumber = "264",
    scryfallId = "b34000e9-ff20-4fb4-9d0b-03a172a92457",
    artist = "Sergey Glushakov",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b34000e9-ff20-4fb4-9d0b-03a172a92457.jpg?1775938845",
    releaseDate = "2026-04-24",
    rarity = Rarity.RARE,
)
