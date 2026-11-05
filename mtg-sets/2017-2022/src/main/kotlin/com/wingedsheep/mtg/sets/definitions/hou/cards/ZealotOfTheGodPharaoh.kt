package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ZealotOfTheGodPharaoh = card("Zealot of the God-Pharaoh") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Archer"
    oracleText = "{4}{R}: This creature deals 2 damage to target opponent or planeswalker."
    power = 4
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{4}{R}")
        val t = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "207"
        artist = "Gabor Szikszai"
        flavorText = "The God-Pharaoh's most devoted took it upon themselves to follow his example."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/180d1cd4-7d0d-4279-bc2a-59bb21b06904.jpg?1783935984"
        inBooster = false
    }
}
