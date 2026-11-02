package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gird for Battle
 * {W}
 * Sorcery
 * Put a +1/+1 counter on each of up to two target creatures.
 */
val GirdForBattle = card("Gird for Battle") {
    manaCost = "{W}"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each of up to two target creatures."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Jakub Kasper"
        flavorText = "\"You won't need the sword drills, your shields, or my blessing. You'll fight for Sunhome with the truth in your heart.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9f731df-eb40-44c8-a72d-f377c824584d.jpg?1783934202"
    }
}
