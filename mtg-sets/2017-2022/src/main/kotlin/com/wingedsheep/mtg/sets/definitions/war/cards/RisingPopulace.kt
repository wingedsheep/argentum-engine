package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rising Populace
 * {2}{W}
 * Creature — Human
 * 2/2
 * Whenever another creature or planeswalker you control dies, put a +1/+1 counter on this creature.
 */
val RisingPopulace = card("Rising Populace") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 2
    toughness = 2
    oracleText = "Whenever another creature or planeswalker you control dies, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrPlaneswalker.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Tomasz Jedruszek"
        flavorText = "Grief is a crucible where resolve is tempered."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edcacc28-aacf-4d40-b8cc-f8c028380340.jpg?1783933474"
    }
}
