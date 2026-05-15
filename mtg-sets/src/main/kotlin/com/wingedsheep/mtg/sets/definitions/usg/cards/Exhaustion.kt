package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Exhaustion reprint in USG.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the USG-specific
 * presentation row.
 */
val ExhaustionReprint = Printing(
    oracleId = "0e7b9caf-8285-4386-98bc-9a809827f447",
    name = "Exhaustion",
    setCode = "USG",
    collectorNumber = "74",
    scryfallId = "666efc89-b566-4b2b-a0e2-52f1dedc9e10",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/6/6/666efc89-b566-4b2b-a0e2-52f1dedc9e10.jpg?1562916425",
    releaseDate = "1998-10-12",
    rarity = Rarity.UNCOMMON,
)
