package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Savvy Profiteer
 * {1}{R}
 * Creature — Ferengi Rogue
 * 2/1
 *
 * {T}: Add {R}.
 * {3}, {T}: Create a Treasure token.
 */
val SavvyProfiteer = card("Savvy Profiteer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ferengi Rogue"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {R}.\n" +
        "{3}, {T}: Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Andreia Ugrai"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8256376f-011f-4cc8-9be4-47f5121ca75e.jpg?1793359515"
    }
}
