package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reckless Lackey
 * {R}
 * Creature — Goblin Pirate
 * 1/2
 * First strike, haste
 * {2}{R}, Sacrifice this creature: Draw a card and create a Treasure token. (It's an artifact with
 * "{T}, Sacrifice this token: Add one mana of any color.")
 */
val RecklessLackey = card("Reckless Lackey") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    power = 1
    toughness = 2
    oracleText = "First strike, haste\n" +
        "{2}{R}, Sacrifice this creature: Draw a card and create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")"

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.SacrificeSelf)
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.CreateTreasure(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "\"First to the fray, first to the pay!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/912fcd14-5e81-418c-997b-771f2f38f63d.jpg?1712355825"
    }
}
