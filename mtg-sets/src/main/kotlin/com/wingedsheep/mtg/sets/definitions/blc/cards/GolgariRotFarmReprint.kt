package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Golgari Rot Farm reprint in Bloomburrow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val GolgariRotFarmReprint = Printing(
    oracleId = "1b301478-b14f-4ef8-94e6-9647d582eabe",
    name = "Golgari Rot Farm",
    setCode = "BLC",
    collectorNumber = "308",
    scryfallId = "5fe6e199-26b5-4744-9748-f64ecadefc2f",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5fe6e199-26b5-4744-9748-f64ecadefc2f.jpg?1783910638",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
