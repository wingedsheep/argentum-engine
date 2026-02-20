package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wall of Mulch
 * {1}{G}
 * Creature — Wall
 * 0/4
 * Defender (This creature can't attack.)
 * {G}, Sacrifice a Wall: Draw a card.
 */
val WallOfMulch = card("Wall of Mulch") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender\n{G}, Sacrifice a Wall: Draw a card."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Wall"))
        )
        effect = DrawCardsEffect(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "298"
        artist = "Anthony S. Waters"
        flavorText = "Mulch is the foundation on which the forest is built and rebuilt time and again."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b3b4448-50f0-4996-94a1-db9ce356d925.jpg?1562927857"
    }
}
