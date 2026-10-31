package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Destructive Digger (M20 #134)
 * {2}{R}  Creature — Goblin  3/2
 *
 * {3}, {T}, Sacrifice an artifact or land: Draw a card.
 */
val DestructiveDigger = card("Destructive Digger") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "{3}, {T}, Sacrifice an artifact or land: Draw a card."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.ArtifactOrLand)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Zoltan Boros"
        flavorText = "\"I love digging. It gives me lots of time to think about how much I love digging.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d981b310-5e75-4f30-baf6-37f069f00aa3.jpg"
    }
}
