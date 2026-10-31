package com.wingedsheep.mtg.sets.definitions.m20.cards

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
 * Vivien's Crocodile (M20 #301)
 * {2}{G}  Creature — Crocodile Spirit  3/3
 *
 * This creature gets +1/+1 as long as you control a Vivien planeswalker.
 */
val ViviensCrocodile = card("Vivien's Crocodile") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Crocodile Spirit"
    oracleText = "This creature gets +1/+1 as long as you control a Vivien planeswalker."
    power = 3
    toughness = 3

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Vivien")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "301"
        artist = "Bayard Wu"
        flavorText = "\"Regardless of where they come from, crocs are usually able to find their way home. For this one, home is me.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c51c6bd-2ecc-4d92-a406-fcb31e889b45.jpg"
    }
}
