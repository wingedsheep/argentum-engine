package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Consume Spirit reprint in Magic 2010. Canonical CardDefinition lives in Mirrodin (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.ConsumeSpirit`.
 */
val ConsumeSpiritM10Reprint = Printing(
    oracleId = "861fa80d-99e0-4332-a2b8-5aa959fd41a4",
    name = "Consume Spirit",
    setCode = "M10",
    collectorNumber = "89",
    scryfallId = "5db2f958-947b-4b52-a5cd-e8f8b5576803",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5db2f958-947b-4b52-a5cd-e8f8b5576803.jpg?1783942385",
    releaseDate = "2009-07-17",
    rarity = Rarity.UNCOMMON,
)
