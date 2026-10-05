package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility

/**
 * Tome Anima
 * {3}{U}
 * Creature — Spirit
 * 3/3
 * This creature can't be blocked as long as you've drawn two or more cards this turn.
 *
 * A source-scoped [CantBeBlocked] gated on [Conditions.YouDrewCardsThisTurn] (threshold 2).
 */
val TomeAnima = card("Tome Anima") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked as long as you've drawn two or more cards this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Johannes Voss"
        flavorText = "Sometimes knowledge takes on a life of its own."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0360d27b-37e1-4e00-9cfc-b574efc38ea0.jpg?1783930716"
    }
}
