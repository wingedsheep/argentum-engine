package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sunrise Cavalier
 * {1}{R}{W}
 * Creature — Human Knight
 * 3/3
 * Trample, haste
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, put a +1/+1 counter on target creature you control.
 */
val SunriseCavalier = card("Sunrise Cavalier") {
    manaCost = "{1}{R}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Trample, haste\n" +
        "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, put a +1/+1 counter on target creature you control."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "John Di Giovanni"
        flavorText = "\"Like us, the morning fights for survival.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d0081b1-40fd-44ad-b154-efe2f27fed6b.jpg?1783925552"
    }
}
