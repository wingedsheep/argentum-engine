package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Sapphire Drake
 * {5}{U}
 * Creature — Drake
 * 4/4
 * Flying
 * Each creature you control with a +1/+1 counter on it has flying.
 */
val SapphireDrake = card("Sapphire Drake") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 4
    toughness = 4
    oracleText = "Flying\nEach creature you control with a +1/+1 counter on it has flying."

    keywords(Keyword.FLYING)

    // Each creature you control with a +1/+1 counter on it has flying.
    staticAbility { ability = GrantKeywordByCounter(Keyword.FLYING, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Steve Prescott"
        flavorText = "Sapphire drakes can communicate over great distances, their calls resonating in the gems fixed in their skulls."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0fe14cf-5d34-47d1-9071-4a532819719b.jpg?1783940135"
    }
}
