package com.wingedsheep.mtg.sets.definitions.eld.cards

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
 * Rowan's Battleguard
 * {3}{R}
 * Creature — Human Knight
 * 3/3
 *
 * First strike
 * As long as you control a Rowan planeswalker, this creature gets +3/+0.
 */
val RowansBattleguard = card("Rowan's Battleguard") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    oracleText = "First strike\n" +
        "As long as you control a Rowan planeswalker, this creature gets +3/+0."
    power = 3
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 0, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Rowan")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "306"
        artist = "Sara Winters"
        flavorText = "\"This sword is Sunrise. The other is Dawn. They mark the beginning of my days but the end of yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c8a3119-ce9b-4fe5-aea3-871a7eee216e.jpg"
    }
}
