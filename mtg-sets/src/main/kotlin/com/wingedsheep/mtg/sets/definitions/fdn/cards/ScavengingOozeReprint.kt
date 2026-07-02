package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scavenging Ooze reprint in FDN. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Commander 2011's `cards/` package;
 * this file contributes only the FDN presentation row.
 */
val ScavengingOozeReprint = Printing(
    oracleId = "1ff25f67-36a7-4cfa-a2b1-2135b5b6fb67",
    name = "Scavenging Ooze",
    setCode = "FDN",
    collectorNumber = "232",
    scryfallId = "8c504c23-1e9a-411b-9cfe-4180d0c744f6",
    artist = "Austin Hsu",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c504c23-1e9a-411b-9cfe-4180d0c744f6.jpg?1782689065",
    releaseDate = "2024-11-15",
    rarity = Rarity.RARE,
)
