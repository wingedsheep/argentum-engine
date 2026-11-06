package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani's Comrade
 * {1}{G}
 * Creature — Elf Soldier
 * 2/2
 * Trample
 * At the beginning of combat on your turn, if you control an Ajani planeswalker, put a +1/+1
 * counter on this creature.
 */
val AjanisComrade = card("Ajani's Comrade") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Soldier"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "At the beginning of combat on your turn, if you control an Ajani planeswalker, put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Ajani"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "James Ryman"
        flavorText = "\"I will go where the lion-man leads.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bebef1c-2f03-4e0a-b3ba-861546ebf8a6.jpg?1783936714"
    }
}
