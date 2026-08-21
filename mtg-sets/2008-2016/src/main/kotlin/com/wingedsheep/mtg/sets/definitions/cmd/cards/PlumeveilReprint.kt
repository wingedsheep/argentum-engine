package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plumeveil reprint in Commander. The canonical CardDefinition lives in Shadowmoor
 * (`definitions/shm/cards/Plumeveil.kt`); this file contributes only per-printing presentation data.
 */
val PlumeveilReprint = Printing(
    oracleId = "14fa0771-cf2b-4563-98b2-ff4ee24bce21",
    name = "Plumeveil",
    setCode = "CMD",
    collectorNumber = "218",
    scryfallId = "6b2d484d-b0fd-4add-a54f-0956d7401950",
    artist = "Nils Hamm",
    imageUri = "https://cards.scryfall.io/normal/front/6/b/6b2d484d-b0fd-4add-a54f-0956d7401950.jpg?1783941172",
    releaseDate = "2011-06-17",
    rarity = Rarity.UNCOMMON,
)
