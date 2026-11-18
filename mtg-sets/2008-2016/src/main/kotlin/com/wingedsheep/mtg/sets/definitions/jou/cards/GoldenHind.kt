package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Golden Hind
 * {1}{G}
 * Creature — Elk
 * 2/1
 *
 * {T}: Add {G}.
 */
val GoldenHind = card("Golden Hind") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elk"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {G}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Cyril Van Der Haegen"
        flavorText = "\"It is not yours to hunt, mortal. Be content with its beauty alone.\"\n—Nylea, god of the hunt"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ae883ea-191e-4571-be3d-1e3149e6965e.jpg?1783939414"
    }
}
