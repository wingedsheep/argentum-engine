package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Storm Fleet Aerialist
 * {1}{U}
 * Creature — Human Pirate
 * 1/2
 * Flying
 * Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.
 *
 * Raid is an ability word; the replacement effect reads [Conditions.YouAttackedThisTurn] as the
 * creature enters.
 */
val StormFleetAerialist = card("Storm Fleet Aerialist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "Raid — This creature enters with a +1/+1 counter on it if you attacked this turn."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = Conditions.YouAttackedThisTurn
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Jesper Ejsing"
        flavorText = "\"Only one thing beats the freedom of the sea: the freedom of the sky.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9de1acd5-bee4-48fe-951d-7936e20f0a71.jpg?1783935769"
    }
}
