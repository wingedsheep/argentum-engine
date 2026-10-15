package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brokers Initiate
 * {W}
 * Creature — Cat Citizen
 * 0 / 4
 * {4}{G/U}: This creature has base power and toughness 5/5 until end of turn.
 */
val BrokersInitiate = card("Brokers Initiate") {
    manaCost = "{W}"
    colorIdentity = "GUW"
    typeLine = "Creature — Cat Citizen"
    oracleText = "{4}{G/U}: This creature has base power and toughness 5/5 until end of turn."
    power = 0
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{4}{G/U}")
        effect = Effects.SetBasePowerAndToughness(5, 5, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Viko Menezes"
        flavorText = "Regarding the relative might of pen and sword, the official position of the Brokers is \"Why choose?\""
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7867f2d-e89d-429f-9ab8-5337b40b4d8d.jpg"
    }
}
