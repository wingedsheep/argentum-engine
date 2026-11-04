package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rekindling Phoenix reprint in New Capenna Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Rivals of Ixalan's `cards/` package; this
 * file contributes only the New Capenna Commander presentation row.
 */
val RekindlingPhoenixReprint = Printing(
    oracleId = "f34be4b8-126d-4b5c-8ebb-23b5e5370012",
    name = "Rekindling Phoenix",
    setCode = "NCC",
    collectorNumber = "273",
    scryfallId = "1a9945ec-0996-434e-b177-97203eabaf24",
    artist = "Jason Rainville",
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1a9945ec-0996-434e-b177-97203eabaf24.jpg",
    releaseDate = "2022-04-29",
    rarity = Rarity.MYTHIC,
)
