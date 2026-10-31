package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rankle, Master of Pranks reprints in CMM — the deck printing (#180) and the etched foil printing
 * (#519). Canonical CardDefinition lives in its earliest set (ELD).
 */
val RankleMasterOfPranksReprint = Printing(
    oracleId = "b8619990-9dc2-4fcc-bc7e-457b77cd2a8e",
    name = "Rankle, Master of Pranks",
    setCode = "CMM",
    collectorNumber = "180",
    scryfallId = "32399c54-632f-434e-bd5a-81b6a7c4fe9b",
    artist = "Dmitry Burmak",
    imageUri = "https://cards.scryfall.io/normal/front/3/2/32399c54-632f-434e-bd5a-81b6a7c4fe9b.jpg?1783915667",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)

val RankleMasterOfPranksEtchedReprint = Printing(
    oracleId = "b8619990-9dc2-4fcc-bc7e-457b77cd2a8e",
    name = "Rankle, Master of Pranks",
    setCode = "CMM",
    collectorNumber = "519",
    scryfallId = "9e793c0f-22f9-4ed5-9fb8-bd29f540ee7c",
    artist = "Dmitry Burmak",
    imageUri = "https://cards.scryfall.io/normal/front/9/e/9e793c0f-22f9-4ed5-9fb8-bd29f540ee7c.jpg?1783915552",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)
