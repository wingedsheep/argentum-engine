package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Swaggering Corsair
 * {2}{R}
 * Creature — Human Pirate
 * 2/2
 *
 * Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.
 */
val SwaggeringCorsair = card("Swaggering Corsair") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Raid — This creature enters with a +1/+1 counter on it if you attacked this turn."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.YouAttackedThisTurn
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Scott Murphy"
        flavorText = "\"I'm about to make you famous. From the golden city to High and Dry, they'll talk about how fast you died!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22da9f6a-9598-4cfe-b465-719266b65dee.jpg?1783935291"
    }
}
