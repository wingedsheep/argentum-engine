package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nomad Mythmaker reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * JUD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NomadMythmakerReprint = Printing(
    oracleId = "591ce7dc-8407-43ed-acfc-e3d8fd547125",
    name = "Nomad Mythmaker",
    setCode = "10E",
    collectorNumber = "30",
    artist = "Darrell Riche",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/0468f109-8020-4a74-8ffd-bba771362f76.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
