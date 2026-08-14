package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Voice of the Provinces reprint in JMP. Canonical CardDefinition lives in Avacyn Restored (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.VoiceOfTheProvinces`.
 */
val VoiceOfTheProvincesJmpReprint = Printing(
    oracleId = "81b15ed1-7069-4f8b-96b9-f6d67298afef",
    name = "Voice of the Provinces",
    setCode = "JMP",
    collectorNumber = "137",
    scryfallId = "30a78066-c52e-48fd-bcf9-d0b60f00fddc",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/30a78066-c52e-48fd-bcf9-d0b60f00fddc.jpg?1783930461",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
