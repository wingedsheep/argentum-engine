package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Emancipation Angel reprint in JMP. Canonical CardDefinition lives in Avacyn Restored (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.EmancipationAngel`.
 */
val EmancipationAngelJmpReprint = Printing(
    oracleId = "3834d3b0-9b66-465a-a8dc-22875a819fd9",
    name = "Emancipation Angel",
    setCode = "JMP",
    collectorNumber = "102",
    scryfallId = "9b2a972a-a953-485d-920d-8f4f978ef758",
    artist = "Scott Chou",
    imageUri = "https://cards.scryfall.io/normal/front/9/b/9b2a972a-a953-485d-920d-8f4f978ef758.jpg?1783930474",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)
