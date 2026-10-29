package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Warded Battlements (Core Set 2021 #44)
 * {2}{W} · Creature — Wall · 0/3
 *
 * Defender
 * Attacking creatures you control get +1/+0.
 */
val WardedBattlements = card("Warded Battlements") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\nAttacking creatures you control get +1/+0."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Titus Lunter"
        flavorText = "In dire times, even a place of refuge can become a weapon of war."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f234999b-54e9-40f5-a537-7d6ce169c710.jpg?1783930731"
    }
}
