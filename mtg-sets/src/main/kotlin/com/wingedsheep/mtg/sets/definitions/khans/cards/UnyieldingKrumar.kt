package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unyielding Krumar
 * {3}{B}
 * Creature — Orc Warrior
 * 3/3
 * {1}{W}: Unyielding Krumar gains first strike until end of turn.
 */
val UnyieldingKrumar = card("Unyielding Krumar") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Orc Warrior"
    power = 3
    toughness = 3
    oracleText = "{1}{W}: This creature gains first strike until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Viktor Titov"
        flavorText = "\"The man whom I call father killed the orc who sired me, offering his world and his blade in return.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a232d196-490d-4712-b2a2-466751b28d11.jpg?1562791316"
    }
}
