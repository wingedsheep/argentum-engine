package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Merfolk Skydiver
 * {G}{U}
 * Creature — Merfolk Mutant
 * 1/1
 * Flying
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * {3}{G}{U}: Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val MerfolkSkydiver = card("Merfolk Skydiver") {
    manaCost = "{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Merfolk Mutant"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "{3}{G}{U}: Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{U}")
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bf211eb-837b-4c02-b02e-8d9e62f1abb1.jpg?1783933392"
    }
}
