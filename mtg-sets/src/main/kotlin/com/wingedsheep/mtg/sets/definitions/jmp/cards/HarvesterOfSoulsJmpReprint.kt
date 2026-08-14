package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Harvester of Souls reprint in JMP. Canonical CardDefinition lives in Avacyn Restored (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.HarvesterOfSouls`.
 */
val HarvesterOfSoulsJmpReprint = Printing(
    oracleId = "5987ce77-10ad-4871-900a-5a005fcf4955",
    name = "Harvester of Souls",
    setCode = "JMP",
    collectorNumber = "243",
    scryfallId = "870ebc0b-b748-4a21-b939-a48811451bba",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/8/7/870ebc0b-b748-4a21-b939-a48811451bba.jpg?1783930420",
    releaseDate = "2020-07-17",
    rarity = Rarity.RARE,
)
