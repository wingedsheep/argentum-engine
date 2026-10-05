package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Murder Investigation reprint in J22. Canonical CardDefinition lives in Gatecrash (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.gtc.cards.MurderInvestigation`.
 */
val MurderInvestigationReprint = Printing(
    oracleId = "42ca56c5-a9c4-4a2c-ae05-bce47aa6e16c",
    name = "Murder Investigation",
    setCode = "J22",
    collectorNumber = "219",
    scryfallId = "612da129-c8dd-4f89-a505-548ad5c2f7e1",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/612da129-c8dd-4f89-a505-548ad5c2f7e1.jpg?1783919099",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
