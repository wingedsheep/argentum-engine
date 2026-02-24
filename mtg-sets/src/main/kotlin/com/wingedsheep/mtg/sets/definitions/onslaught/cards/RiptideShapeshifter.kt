package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Riptide Shapeshifter
 * {3}{U}{U}
 * Creature — Shapeshifter
 * 3/3
 * {2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from the top
 * of your library until you reveal a creature card of that type. Put that card onto the
 * battlefield and shuffle the rest into your library.
 */
val RiptideShapeshifter = card("Riptide Shapeshifter") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "{2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from the top of your library until you reveal a creature card of that type. Put that card onto the battlefield and shuffle the rest into your library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.SacrificeSelf)
        effect = EffectPatterns.revealUntilCreatureTypeToBattlefield()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85be34ac-7bc2-4da2-8d9c-2412b9946073.jpg?1562926477"
    }
}
