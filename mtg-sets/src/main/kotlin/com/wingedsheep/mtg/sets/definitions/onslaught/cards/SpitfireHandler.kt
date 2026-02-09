package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockCreaturesWithGreaterPower
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Spitfire Handler
 * {1}{R}
 * Creature — Goblin
 * 1/1
 * This creature can't block creatures with power greater than this creature's power.
 * {R}: This creature gets +1/+0 until end of turn.
 */
val SpitfireHandler = card("Spitfire Handler") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1

    staticAbility {
        ability = CantBlockCreaturesWithGreaterPower()
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = ModifyStatsEffect(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Jim Nelson"
        flavorText = "\"Wait 'til Toggo sees this!\""
        imageUri = "https://cards.scryfall.io/large/front/e/f/efe72820-952f-4c53-9ee7-ea7ea54fc848.jpg?1595099913"
    }
}
