package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stalwarts of Osgiliath
 * {4}{W}
 * Creature — Human Soldier
 * 4/3
 *
 * When this creature enters, the Ring tempts you.
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 */
val StalwartsOfOsgiliath = card("Stalwarts of Osgiliath") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, the Ring tempts you.\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.TheRingTemptsYou()
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Lixin Yin"
        flavorText = "\"The Enemy must pay dearly for the crossing of the River.\"\n—Denethor"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2acb8240-8e0e-4adf-a884-4986c116e704.jpg?1686967946"
    }
}
