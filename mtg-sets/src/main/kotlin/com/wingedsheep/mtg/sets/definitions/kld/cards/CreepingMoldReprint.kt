package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Creeping Mold reprint in KLD. Canonical CardDefinition lives in Visions (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.vis.cards.CreepingMold`.
 */
val CreepingMoldReprint = Printing(
    oracleId = "59180e94-ccdf-4d9f-9a4a-fe55497d0d63",
    name = "Creeping Mold",
    setCode = "KLD",
    collectorNumber = "150",
    scryfallId = "277b549c-8691-42b2-9867-802b158a506c",
    artist = "Cliff Childs",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/277b549c-8691-42b2-9867-802b158a506c.jpg?1783937182",
    releaseDate = "2016-09-30",
    rarity = Rarity.UNCOMMON,
)
