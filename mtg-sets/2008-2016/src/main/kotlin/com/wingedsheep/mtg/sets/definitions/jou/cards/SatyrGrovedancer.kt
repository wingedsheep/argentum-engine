package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Satyr Grovedancer
 * {1}{G}
 * Creature — Satyr Shaman
 * 1/1
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 */
val SatyrGrovedancer = card("Satyr Grovedancer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Shaman"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Jason A. Engle"
        flavorText = "\"Some of my kin dance for themselves, without greater purpose. Nylea gives me purpose, and with it strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d46febca-f3c0-4467-aef4-4dfcdf3009fe.jpg?1783939410"
    }
}
