package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Monk of the Open Hand
 * {W}
 * Creature — Elf Monk
 * 1/1
 * Flurry of Blows — Whenever you cast your second spell each turn, put a +1/+1 counter on this creature.
 */
val MonkOfTheOpenHand = card("Monk of the Open Hand") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elf Monk"
    oracleText = "Flurry of Blows — Whenever you cast your second spell each turn, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Bryan Sola"
        flavorText = "His mind is a tranquil meadow, his fists a raging storm."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d934765f-f490-4e8d-843d-b2b559ee0418.jpg?1783926528"
    }
}
