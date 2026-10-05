package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jace, Arcane Strategist reprint in J22. Canonical CardDefinition lives in War of the Spark (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.war.cards.JaceArcaneStrategist`.
 */
val JaceArcaneStrategistReprint = Printing(
    oracleId = "4126bab7-6a2a-43b2-ba65-77299f9bb8cf",
    name = "Jace, Arcane Strategist",
    setCode = "J22",
    collectorNumber = "310",
    scryfallId = "5b870094-08bf-41fc-b9d6-e435e02dc4e0",
    artist = "Kieran Yanner",
    imageUri = "https://cards.scryfall.io/normal/front/5/b/5b870094-08bf-41fc-b9d6-e435e02dc4e0.jpg?1783919055",
    releaseDate = "2022-12-02",
    rarity = Rarity.MYTHIC,
)
