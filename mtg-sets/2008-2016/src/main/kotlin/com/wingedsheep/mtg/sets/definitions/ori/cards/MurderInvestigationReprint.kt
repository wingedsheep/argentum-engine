package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Murder Investigation reprint in ORI. Canonical CardDefinition lives in Gatecrash (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.gtc.cards.MurderInvestigation`.
 */
val MurderInvestigationReprint = Printing(
    oracleId = "42ca56c5-a9c4-4a2c-ae05-bce47aa6e16c",
    name = "Murder Investigation",
    setCode = "ORI",
    collectorNumber = "27",
    scryfallId = "07d8d6f7-ed22-4e01-9aeb-c6bc0065c89c",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/0/7/07d8d6f7-ed22-4e01-9aeb-c6bc0065c89c.jpg?1783938359",
    releaseDate = "2015-07-17",
    rarity = Rarity.UNCOMMON,
)
