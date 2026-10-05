package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jace's Scrutiny reprint in J22. Canonical CardDefinition lives in Shadows over Innistrad (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.soi.cards.JacesScrutiny`.
 */
val JacesScrutinyReprint = Printing(
    oracleId = "8578c462-7b2d-4b69-ad10-a2eab44ac98d",
    name = "Jace's Scrutiny",
    setCode = "J22",
    collectorNumber = "311",
    scryfallId = "a7e8e533-350b-4558-98d3-b7fcab3770f5",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7e8e533-350b-4558-98d3-b7fcab3770f5.jpg?1783919054",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
