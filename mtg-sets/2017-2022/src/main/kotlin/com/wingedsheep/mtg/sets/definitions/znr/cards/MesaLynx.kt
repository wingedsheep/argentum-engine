package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Mesa Lynx
 * {1}{W}
 * Creature — Cat
 * 2/1
 * During turns other than yours, this creature gets +0/+2.
 */
val MesaLynx = card("Mesa Lynx") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    oracleText = "During turns other than yours, this creature gets +0/+2."
    power = 2
    toughness = 1
    staticAbility {
        ability = ConditionalStaticAbility(ability = ModifyStats(0, 2, Filters.Self), condition = Conditions.IsNotYourTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Svetlin Velinov"
        flavorText = "Its ferocity is tempered by patience, knowing it must wait for just the right moment to strike a killing blow."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f65af09b-656f-4d81-b95d-b4f902e56bb7.jpg?1783929411"
    }
}
