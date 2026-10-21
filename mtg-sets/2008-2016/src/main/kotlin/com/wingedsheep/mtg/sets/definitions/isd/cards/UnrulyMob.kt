package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unruly Mob
 * {1}{W}
 * Creature — Human
 * 1/1
 * Whenever another creature you control dies, put a +1/+1 counter on this creature.
 */
val UnrulyMob = card("Unruly Mob") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    oracleText = "Whenever another creature you control dies, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Ryan Pancoast"
        flavorText = "\"Who says werewolves are the only ones who can hunt as a pack?\"\n—Kolman, elder of Gatstaf"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/491c6e40-151a-4efd-980c-e6b6a1057c58.jpg"
    }
}
