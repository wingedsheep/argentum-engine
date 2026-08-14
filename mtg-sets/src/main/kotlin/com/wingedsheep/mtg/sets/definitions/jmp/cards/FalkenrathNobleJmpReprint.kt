package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Falkenrath Noble reprint in JMP. Canonical CardDefinition lives in Innistrad (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.isd.cards.FalkenrathNoble`.
 */
val FalkenrathNobleJmpReprint = Printing(
    oracleId = "3739b179-bc81-4737-8376-66a57e16b942",
    name = "Falkenrath Noble",
    setCode = "JMP",
    collectorNumber = "232",
    scryfallId = "a60b3c77-62e4-4718-9ddb-cb2e3f1f861f",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a60b3c77-62e4-4718-9ddb-cb2e3f1f861f.jpg?1783930425",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)
