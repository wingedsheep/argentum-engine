package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thicket Crasher (M20 #196)
 * {3}{G}  Creature — Elemental Rhino  4/3
 *
 * Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)
 * Other Elementals you control have trample.
 */
val ThicketCrasher = card("Thicket Crasher") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Rhino"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\nOther Elementals you control have trample."
    power = 4
    toughness = 3

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Elemental").youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Mathias Kollros"
        flavorText = "\"Tread too hard in the forest and it will tread on you in return.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/534eb449-3f1c-46ec-a965-0f5812e2c93e.jpg"
    }
}
