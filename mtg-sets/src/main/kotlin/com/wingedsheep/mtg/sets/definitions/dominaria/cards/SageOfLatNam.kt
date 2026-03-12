package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sage of Lat-Nam
 * {1}{U}
 * Creature — Human Artificer
 * 1/2
 * {T}, Sacrifice an artifact: Draw a card.
 */
val SageOfLatNam = card("Sage of Lat-Nam") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "{T}, Sacrifice an artifact: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Matt Stewart"
        flavorText = "\"Though it was laid to waste by the brothers, many schools of magic trace their origins to the College of Lat-Nam, including the first Tolarian Academy.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7395b77-f7f7-404b-8639-9db6dc28d558.jpg?1562745828"
    }
}
