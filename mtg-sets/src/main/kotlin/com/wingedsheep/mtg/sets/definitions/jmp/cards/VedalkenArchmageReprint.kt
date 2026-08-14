package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vedalken Archmage reprint in Jumpstart. Canonical CardDefinition lives in Mirrodin (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.VedalkenArchmage`.
 */
val VedalkenArchmageJmpReprint = Printing(
    oracleId = "568cf486-0261-4634-ac36-a6507101b2d0",
    name = "Vedalken Archmage",
    setCode = "JMP",
    collectorNumber = "187",
    scryfallId = "508b3cb7-b434-4524-8ef0-7db7f7f22edd",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/5/0/508b3cb7-b434-4524-8ef0-7db7f7f22edd.jpg?1783930442",
    releaseDate = "2020-07-17",
    rarity = Rarity.RARE,
)
