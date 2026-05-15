package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Esika's Chariot reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * KHM's `cards/` package (the card's earliest real printing). This file contributes
 * only the BLC-specific presentation row.
 */
val EsikasChariotReprint = Printing(
    oracleId = "8e7b079d-9ede-421c-bd2b-f9a5126a8e6f",
    name = "Esika's Chariot",
    setCode = "BLC",
    collectorNumber = "215",
    artist = "Raoul Vitale",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1f2514c2-c0c7-44f3-ab9d-48b227f039db.jpg?1721429257",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
