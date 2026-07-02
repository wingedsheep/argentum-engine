package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scavenging Ooze reprint in New Capenna Commander (NCC). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Commander 2011's `cards/` package;
 * this file contributes only the NCC presentation row.
 */
val ScavengingOozeReprint = Printing(
    oracleId = "1ff25f67-36a7-4cfa-a2b1-2135b5b6fb67",
    name = "Scavenging Ooze",
    setCode = "NCC",
    collectorNumber = "309",
    scryfallId = "67d93e17-13fd-4cf5-a53c-a7b6c57a8351",
    artist = "Austin Hsu",
    imageUri = "https://cards.scryfall.io/normal/front/6/7/67d93e17-13fd-4cf5-a53c-a7b6c57a8351.jpg?1782701754",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
