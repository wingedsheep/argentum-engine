package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Courage in Crisis
 * {2}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature, then proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val CourageInCrisis = card("Courage in Crisis") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature, then proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Micah Epstein"
        flavorText = "\"Thanks to my father, I know how to fight. He also showed me why I must.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a22083-9ef9-4ff7-8502-3a77e69299df.jpg?1783933415"
    }
}
