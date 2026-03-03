package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Salt Road Patrol
 * {3}{W}
 * Creature — Human Scout
 * 2/5
 * Outlast {1}{W} ({1}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 */
val SaltRoadPatrol = card("Salt Road Patrol") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 5
    oracleText = "Outlast {1}{W} ({1}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)"

    // Outlast {1}{W}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Scott Murphy"
        flavorText = "\"Soldiers win battles, but supplies win wars.\" —Kadri, Abzan caravan master"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c5ee84d-e5b9-4103-8ba7-ccd79a272c0f.jpg?1562784258"
    }
}
