package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blightbeetle (M20 #87)
 * {1}{B}  Creature — Insect  1/1
 *
 * Protection from green
 * Creatures your opponents control can't have +1/+1 counters put on them.
 */
val Blightbeetle = card("Blightbeetle") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    oracleText = "Protection from green (This creature can't be blocked, targeted, dealt damage, enchanted, or equipped by anything green.)\n" +
        "Creatures your opponents control can't have +1/+1 counters put on them."
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.protectionFrom(Color.GREEN))

    staticAbility {
        ability = CantReceiveCounters(GroupFilter.AllCreaturesOpponentsControl, CounterType.PLUS_ONE_PLUS_ONE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Jason Kang"
        flavorText = "Leaf and soul alike wither at its touch."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd43d44b-de27-4139-9cb8-b1f4c04fb87e.jpg"
    }
}
