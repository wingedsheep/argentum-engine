package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Falkenrath Noble reprint in VOC. Canonical CardDefinition lives in Innistrad (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.isd.cards.FalkenrathNoble`.
 */
val FalkenrathNobleVocReprint = Printing(
    oracleId = "3739b179-bc81-4737-8376-66a57e16b942",
    name = "Falkenrath Noble",
    setCode = "VOC",
    collectorNumber = "128",
    scryfallId = "89420882-5833-46f0-9a3f-3c8e694074df",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89420882-5833-46f0-9a3f-3c8e694074df.jpg?1783924955",
    releaseDate = "2021-11-19",
    rarity = Rarity.UNCOMMON,
)
