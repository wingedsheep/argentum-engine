package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Volcanic Rambler
 * {5}{R}
 * Creature — Elemental
 * 6/4
 * {2}{R}: This creature deals 1 damage to target player or planeswalker.
 */
val VolcanicRambler = card("Volcanic Rambler") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 4
    oracleText = "{2}{R}: This creature deals 1 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val t = target("target player or planeswalker", Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Vincent Proce"
        flavorText = "It moves through lava with the force of an erupting volcano."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fc075ee-16e9-4cf5-bbb0-7b3b4b9eb3f4.jpg?1783938326"
    }
}
