package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gavony Unhallowed
 * {3}{B}
 * Creature — Zombie
 * 2/4
 * Whenever another creature you control dies, put a +1/+1 counter on this creature.
 *
 * Voracious Vermin's death trigger: an `OTHER`-binding battlefield → graveyard trigger, one
 * counter per other creature you control that dies.
 */
val GavonyUnhallowed = card("Gavony Unhallowed") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    oracleText = "Whenever another creature you control dies, put a +1/+1 counter on this creature."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Christopher Moeller"
        flavorText = "\"We entered the church in search of solace, but it became clear we wouldn't find it there.\"\n—Grete, Order of Saint Traft"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21f4c713-089b-497c-8c0f-826cab10aa87.jpg"
    }
}
