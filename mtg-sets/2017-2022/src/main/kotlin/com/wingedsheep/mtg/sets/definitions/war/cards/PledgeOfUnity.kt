package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pledge of Unity
 * {1}{G}{W}
 * Instant
 * Put a +1/+1 counter on each creature you control. You gain 1 life for each creature you control.
 */
val PledgeOfUnity = card("Pledge of Unity") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "WG"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on each creature you control. You gain 1 life for each creature you control."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        ) then Effects.GainLife(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Chris Rallis"
        flavorText = "\"No one hero will save this day. Today we must all be heroes.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca9b3518-7fcd-49e2-a4c4-e5daa8be6c21.jpg?1783933391"
    }
}
