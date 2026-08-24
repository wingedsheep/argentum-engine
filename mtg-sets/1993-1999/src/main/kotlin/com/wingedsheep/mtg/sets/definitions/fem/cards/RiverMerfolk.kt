package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * River Merfolk
 * {U}{U}
 * Creature — Merfolk
 * 2/1
 * {U}: This creature gains mountainwalk until end of turn.
 */
val RiverMerfolk = card("River Merfolk") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "{U}: This creature gains mountainwalk until end of turn. (It can't be blocked " +
        "as long as defending player controls a Mountain.)"
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.MOUNTAINWALK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Douglas Shuler"
        flavorText = "\"Dwelling in icy mountain streams near Goblin and Orcish foes, the River Merfolk were known for their stoicism.\"\n—*Sarpadian Empires, vol. V*"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27d7fa54-4b89-4a9a-b088-4b89c525c1ea.jpg?1783947910"
    }
}
