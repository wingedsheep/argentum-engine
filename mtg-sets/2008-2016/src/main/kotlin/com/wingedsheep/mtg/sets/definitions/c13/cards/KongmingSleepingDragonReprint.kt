package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kongming, "Sleeping Dragon" reprint in C13. Canonical CardDefinition lives in its earliest set
 * (Portal Three Kingdoms, `ptk`).
 *
 * Hand-written rather than generated: `scripts/generate-reprints.py` matches cards by an exact-name
 * Scryfall query, and this card's name embeds literal double quotes, which that query cannot express.
 */
val KongmingSleepingDragonReprint = Printing(
    oracleId = "21e9e1a9-5d6d-473e-adab-6a1e8e2b0ebd",
    name = "Kongming, \"Sleeping Dragon\"",
    setCode = "C13",
    collectorNumber = "16",
    scryfallId = "5432b863-21cc-4898-9463-29049f939e51",
    artist = "Gao Yan",
    imageUri = "https://cards.scryfall.io/normal/front/5/4/5432b863-21cc-4898-9463-29049f939e51.jpg",
    releaseDate = "2013-11-01",
    rarity = Rarity.RARE,
)
