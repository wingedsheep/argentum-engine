package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boa Constrictor
 * {4}{G}
 * Creature — Snake
 * 3 / 3
 * {T}: This creature gets +3/+3 until end of turn.
 */
val BoaConstrictor = card("Boa Constrictor") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    oracleText = "{T}: This creature gets +3/+3 until end of turn."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Carl Critchlow"
        flavorText = "Its eyes are bigger than its stomach, but its mouth is larger still."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7369cbf-6986-4a39-b07c-a283b40aee40.jpg"
    }
}
