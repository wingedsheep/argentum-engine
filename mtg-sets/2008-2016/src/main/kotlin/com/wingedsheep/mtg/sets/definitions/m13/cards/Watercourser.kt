package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Watercourser
 * {2}{U}
 * Creature — Elemental
 * 2/3
 * {U}: This creature gets +1/-1 until end of turn.
 */
val Watercourser = card("Watercourser") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 3
    oracleText = "{U}: This creature gets +1/-1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Mathias Kollros"
        flavorText = "\"Beware an eddy where there should be none or a stretch that flows too fast or too slow.\"\n—Old Fishbones, Martyne river guide"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a27c441a-b31d-4214-8fc5-054003e257dc.jpg?1783940499"
    }
}
