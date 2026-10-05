package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basri's Acolyte
 * {2}{W}{W}
 * Creature — Cat Cleric
 * 2/3
 *
 * Lifelink
 * When this creature enters, put a +1/+1 counter on each of up to two other
 * target creatures you control.
 *
 * The ETB takes an optional `count = 2` target slot (0–2 distinct targets — the same
 * creature can't be chosen twice, per the 2020-06-23 ruling) filtered to
 * [TargetFilter.OtherCreatureYouControl], fanned out with `ForEachTarget` so each chosen
 * creature receives one +1/+1 counter.
 */
val BasrisAcolyte = card("Basri's Acolyte") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    power = 2
    toughness = 3
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "When this creature enters, put a +1/+1 counter on each of up to two other target creatures you control."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two other target creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Leesha Hannigan"
        flavorText = "Basri carries on his god's legacy, spreading her teachings throughout the Multiverse."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08d1dd97-2675-4953-ab95-d47d23abfe05.jpg?1783930745"
        ruling("2020-06-23", "You can't target the same creature twice with the triggered ability of Basri's Acolyte to give it two +1/+1 counters.")
    }
}
