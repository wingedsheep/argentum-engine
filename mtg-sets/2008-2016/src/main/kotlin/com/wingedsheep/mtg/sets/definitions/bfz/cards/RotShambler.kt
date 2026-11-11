package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rot Shambler
 * {1}{G}
 * Creature — Fungus
 * 1/1
 * Whenever another creature you control dies, put a +1/+1 counter on this creature.
 */
val RotShambler = card("Rot Shambler") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    power = 1
    toughness = 1
    oracleText = "Whenever another creature you control dies, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Yeong-Hao Han"
        flavorText = "\"If I die, feed me to a shambler. At least that way I'll be doing some good.\"\n—Ryza, Oran-Rief scout"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a403b28-f91a-4637-af72-d6fc86c3bb9a.jpg?1783938185"
    }
}
