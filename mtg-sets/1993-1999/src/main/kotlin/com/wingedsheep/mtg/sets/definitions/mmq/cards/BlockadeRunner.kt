package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blockade Runner
 * {3}{U}
 * Creature — Merfolk
 * 2 / 2
 */
val BlockadeRunner = card("Blockade Runner") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "{U}: This creature can't be blocked this turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Carl Critchlow"
        flavorText = "\"I need to get back to Mercadia City,\" said Sisay. \"We can get you there,\" the vizier answered. \"Easily.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59e483df-b58a-401e-85bc-0afda4bf7cac.jpg"
    }
}
