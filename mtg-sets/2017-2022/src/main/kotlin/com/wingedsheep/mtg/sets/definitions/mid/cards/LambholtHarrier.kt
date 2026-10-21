package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lambholt Harrier
 * {1}{R}
 * Creature — Wolf
 * 2/2
 * {3}{R}: Target creature can't block this turn.
 */
val LambholtHarrier = card("Lambholt Harrier") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 2
    oracleText = "{3}{R}: Target creature can't block this turn."

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        val t = target(TargetFilter.Creature)
        effect = Effects.CantBlock(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Jason Kang"
        flavorText = "\"This Tovolar is no brainless brute. He sends his howling minions to terrorize a " +
            "village for months. Sleepless nights and fear take their toll. Only then, when morale is at " +
            "its worst, does he strike.\"\n—Barnes, village watch"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a732ca-cd68-465f-8e44-783e1fe34e44.jpg"
    }
}
