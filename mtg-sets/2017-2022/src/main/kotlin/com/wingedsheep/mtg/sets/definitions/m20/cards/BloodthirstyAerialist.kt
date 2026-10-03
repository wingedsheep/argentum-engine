package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodthirsty Aerialist
 * {1}{B}{B}
 * Creature — Vampire Rogue
 * 2/3
 * Flying
 * Whenever you gain life, put a +1/+1 counter on this creature.
 */
val BloodthirstyAerialist = card("Bloodthirsty Aerialist") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Flying\nWhenever you gain life, put a +1/+1 counter on this creature."
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Igor Kieryluk"
        flavorText = "A circus acrobat in life, she kills with such fluid grace that even her victims are awed by her performance."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29156f28-38d8-4f65-b416-089f3fd97109.jpg"
    }
}
