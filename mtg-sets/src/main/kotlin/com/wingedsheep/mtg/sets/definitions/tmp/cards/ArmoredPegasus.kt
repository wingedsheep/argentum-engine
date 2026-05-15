package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Armored Pegasus reprint in TMP.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the TMP-specific
 * presentation row.
 */
val ArmoredPegasusReprint = Printing(
    oracleId = "f097a059-5505-4c3c-b879-7853ab6972ed",
    name = "Armored Pegasus",
    setCode = "TMP",
    collectorNumber = "5",
    scryfallId = "012049f8-0936-49ed-948d-0d34af28550f",
    artist = "Una Fricker",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/012049f8-0936-49ed-948d-0d34af28550f.jpg?1562052320",
    releaseDate = "1997-10-14",
    rarity = Rarity.COMMON,
)
