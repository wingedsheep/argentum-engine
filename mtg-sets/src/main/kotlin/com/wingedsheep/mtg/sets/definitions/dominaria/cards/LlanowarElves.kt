package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Llanowar Elves
 * {G}
 * Creature — Elf Druid
 * 1/1
 * {T}: Add {G}.
 */
val LlanowarElves = card("Llanowar Elves") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {G}."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Chris Rahn"
        flavorText = "As patient and generous as life, as harsh and merciless as nature."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/581b7327-3215-4a4f-b4ae-d9d4002ba882.jpg?1562736014"
    }
}
