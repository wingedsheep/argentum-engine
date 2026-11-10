package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Munda's Vanguard
 * {4}{W}
 * Creature — Kor Knight Ally
 * 3/3
 * Cohort — {T}, Tap an untapped Ally you control: Put a +1/+1 counter on each creature you control.
 */
val MundasVanguard = card("Munda's Vanguard") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Knight Ally"
    power = 3
    toughness = 3
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Put a +1/+1 counter on each creature you control."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Steve Prescott"
        flavorText = "\"Do not let yourselves fall now. Zendikar is depending on us—on you!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dfb21b1-95a5-446d-afc6-aede81ee5a0d.jpg"
    }
}
