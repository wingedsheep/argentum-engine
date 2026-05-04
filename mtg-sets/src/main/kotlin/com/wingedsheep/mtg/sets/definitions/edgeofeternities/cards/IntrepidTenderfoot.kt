package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intrepid Tenderfoot
 * {1}{G}
 * Creature — Insect Citizen
 * {3}: Put a +1/+1 counter on this creature. Activate only as a sorcery.
 */
val IntrepidTenderfoot = card("Intrepid Tenderfoot") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Insect Citizen"
    power = 2
    toughness = 2
    oracleText = "{3}: Put a +1/+1 counter on this creature. Activate only as a sorcery."

    // {3}: Put a +1/+1 counter on this creature. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "{3}: Put a +1/+1 counter on this creature. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Xavier Ribeiro"
        flavorText = "Narix felt their molt coming. Before taking a new form, they had to find what Evendo required of them."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/809df0ea-deff-47b9-83df-cc1f360d377e.jpg?1752947342"
    }
}
