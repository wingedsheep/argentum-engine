package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leaping Master
 * {1}{R}
 * Creature — Human Monk
 * 2/1
 * {2}{W}: Leaping Master gains flying until end of turn.
 */
val LeapingMaster = card("Leaping Master") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 1
    oracleText = "{2}{W}: Leaping Master gains flying until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Strength batters down barriers. Discipline ignores them.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f786a1d-4703-4bac-abba-632506d2726c.jpg?1562782598"
    }
}
