package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Celestial Messenger (M20 #287)
 * {2}{U}{U}  Creature — Bird Spirit  3/2
 *
 * Flash
 * Flying
 * This creature gets +1/+1 as long as you control a Yanling planeswalker.
 */
val CelestialMessenger = card("Celestial Messenger") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Spirit"
    oracleText = "Flash (You may cast this card any time you could cast an instant.)\n" +
        "Flying\n" +
        "This creature gets +1/+1 as long as you control a Yanling planeswalker."
    power = 3
    toughness = 2

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Yanling")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "287"
        artist = "Daniel Ljunggren"
        flavorText = "\"It soars on the winds of dreams.\"\n—Mu Yanling"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e92e98-202f-4790-8411-17aebffe19fe.jpg"
    }
}
