package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Oracle's Vault reprint in NCC. The canonical CardDefinition lives in
 * Amonkhet (`akh`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val OraclesVaultReprint = Printing(
    oracleId = "220e3298-1663-4731-b50f-247076e1328b",
    name = "Oracle's Vault",
    setCode = "NCC",
    collectorNumber = "374",
    scryfallId = "0314bea5-4e04-4e74-9832-3e935eb90ee1",
    artist = "Jonas De Ro",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/0314bea5-4e04-4e74-9832-3e935eb90ee1.jpg?1783923211",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
