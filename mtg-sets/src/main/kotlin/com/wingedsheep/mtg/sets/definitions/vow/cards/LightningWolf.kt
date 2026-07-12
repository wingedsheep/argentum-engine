package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightning Wolf
 * {3}{R}
 * Creature — Wolf
 * 4/3
 * {1}{R}: This creature gains first strike until end of turn. Activate only as a sorcery.
 */
val LightningWolf = card("Lightning Wolf") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    oracleText = "{1}{R}: This creature gains first strike until end of turn. Activate only as a sorcery."
    power = 4
    toughness = 3
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Alessandra Pisano"
        flavorText = "These thick-furred wolves have adapted to Stensia's storms, channeling their energy into bursts of supernatural speed."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7211e4c3-e940-41da-88e4-4630eab447a6.jpg?1782703071"
    }
}
