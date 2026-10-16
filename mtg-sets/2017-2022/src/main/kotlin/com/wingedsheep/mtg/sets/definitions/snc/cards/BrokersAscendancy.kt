package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brokers Ascendancy
 * {G}{W}{U}
 * Enchantment
 *
 * At the beginning of your end step, put a +1/+1 counter on each creature you control and a
 * loyalty counter on each planeswalker you control.
 *
 * Modeling notes:
 *  - Two independent `ForEachInGroup` passes (Abzan Ascendancy's creature pass, Way of the
 *    Mentor's planeswalker pass). Each group is evaluated on its own, so a permanent that is both
 *    a creature and a planeswalker gets both counters (2022-04-29 ruling).
 */
val BrokersAscendancy = card("Brokers Ascendancy") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, put a +1/+1 counter on each creature you control " +
        "and a loyalty counter on each planeswalker you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.youControl()),
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity)
        )
        description = "At the beginning of your end step, put a +1/+1 counter on each creature you " +
            "control and a loyalty counter on each planeswalker you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Dallas Williams"
        flavorText = "\"This is the beginning of a beautiful friendship. And more importantly, a profitable one. Now sign here.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/799de0f1-6bf2-4158-8a4f-bedc3d2f6578.jpg?1783923093"
        ruling("2022-04-29", "A permanent that is somehow both a creature and a planeswalker as Brokers Ascendancy's ability resolves will get both a +1/+1 counter and a loyalty counter.")
    }
}
