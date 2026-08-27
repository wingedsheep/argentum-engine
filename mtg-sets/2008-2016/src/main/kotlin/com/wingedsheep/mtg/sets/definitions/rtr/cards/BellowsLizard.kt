package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bellows Lizard
 * {R}
 * Creature — Lizard
 * 1/1
 * {1}{R}: This creature gets +1/+0 until end of turn.
 */
val BellowsLizard = card("Bellows Lizard") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 1
    toughness = 1
    oracleText = "{1}{R}: This creature gets +1/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Jack Wang"
        flavorText = "As the price of wood and coal rose, smiths found creative ways to keep their forges burning."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5da4a644-9809-4591-9007-6b70b5f9d923.jpg?1783940357"
    }
}
