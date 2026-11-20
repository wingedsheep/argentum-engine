package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Savageborn Hydra
 * {X}{R}{G}
 * Creature — Hydra
 * 0/0
 * Double strike
 * This creature enters with X +1/+1 counters on it.
 * {1}{R/G}: Put a +1/+1 counter on this creature. Activate only as a sorcery.
 */
val SavagebornHydra = card("Savageborn Hydra") {
    manaCost = "{X}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "Double strike\n" +
        "This creature enters with X +1/+1 counters on it.\n" +
        "{1}{R/G}: Put a +1/+1 counter on this creature. Activate only as a sorcery."

    keywords(Keyword.DOUBLE_STRIKE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    activatedAbility {
        cost = Costs.Mana("{1}{R/G}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "100"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f2b73cd-6179-4885-9d92-1782d0b492c1.jpg?1783940021"
    }
}
