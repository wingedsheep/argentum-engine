package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steadfast Sentry (M20 #40)
 * {2}{W}  Creature — Human Soldier  3/2
 *
 * Vigilance (Attacking doesn't cause this creature to tap.)
 * When this creature dies, put a +1/+1 counter on target creature you control.
 */
val SteadfastSentry = card("Steadfast Sentry") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\nWhen this creature dies, put a +1/+1 counter on target creature you control."
    power = 3
    toughness = 2

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Bram Sels"
        flavorText = "\"Get up, slugabeds! We've got company, and they're not dressed for a party!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d92614e-05da-4b5c-a98b-addf8ab2de7b.jpg"
    }
}
