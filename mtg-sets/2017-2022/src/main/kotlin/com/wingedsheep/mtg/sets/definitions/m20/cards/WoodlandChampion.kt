package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Woodland Champion
 * {1}{G}
 * Creature — Elf Scout
 * 2/2
 *
 * Whenever one or more tokens you control enter, put that many +1/+1 counters on this creature.
 *
 * The batched ETB trigger captures the tokens that entered; "that many" is the size of that
 * captured collection, which is fixed at trigger time — per the ruling it still counts tokens
 * that left the battlefield before the ability resolves.
 */
val WoodlandChampion = card("Woodland Champion") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "Whenever one or more tokens you control enter, put that many +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Token).enter()
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.distinctEntitiesIn(IterationSpace.TRIGGER_CAPTURED_COLLECTION),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Randy Vargas"
        flavorText = "\"Every footfall on the forest floor is a heartbeat sending strength into my veins.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/250f5303-428a-478c-9bd5-2410f7fcc4dd.jpg?1783932953"
        ruling("2020-08-07", "The number of +1/+1 counters you put on Woodland Champion is the number of tokens that entered the battlefield under your control, even if some or all of them leave the battlefield before the triggered ability resolves.")
    }
}
