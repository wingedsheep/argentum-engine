package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mysterious Egg
 * {1}
 * Creature — Egg
 * 0/2
 * Whenever this creature mutates, put a +1/+1 counter on it.
 */
val MysteriousEgg = card("Mysterious Egg") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Creature — Egg"
    power = 0
    toughness = 2
    oracleText = "Whenever this creature mutates, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Volkan Baǵa"
        flavorText = "\"A good forage if you can find it, but eat it before you get back to sanctuary. The guards won't let you bring it through the gates.\"\n—Dennae, Savai hunter"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdfac3c3-7f38-49d6-bd88-dc0f5001116f.jpg?1783931094"
    }
}
