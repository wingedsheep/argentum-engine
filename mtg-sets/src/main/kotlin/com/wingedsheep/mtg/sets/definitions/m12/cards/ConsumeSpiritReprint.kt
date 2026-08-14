package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Consume Spirit reprint in Magic 2012. Canonical CardDefinition lives in Mirrodin (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.ConsumeSpirit`.
 */
val ConsumeSpiritM12Reprint = Printing(
    oracleId = "861fa80d-99e0-4332-a2b8-5aa959fd41a4",
    name = "Consume Spirit",
    setCode = "M12",
    collectorNumber = "88",
    scryfallId = "ef144439-fc8e-4844-8ebb-3e36e05ac9a0",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/e/f/ef144439-fc8e-4844-8ebb-3e36e05ac9a0.jpg?1783941083",
    releaseDate = "2011-07-15",
    rarity = Rarity.UNCOMMON,
)
