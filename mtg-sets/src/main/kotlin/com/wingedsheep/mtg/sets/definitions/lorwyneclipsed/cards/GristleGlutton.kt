package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gristle Glutton
 * {1}{R}
 * Creature — Goblin Scout
 * 1/3
 *
 * {T}, Blight 1: Discard a card. If you do, draw a card.
 * (To blight 1, put a -1/-1 counter on a creature you control.)
 */
val GristleGlutton = card("Gristle Glutton") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Scout"
    power = 1
    toughness = 3
    oracleText = "{T}, Blight 1: Discard a card. If you do, draw a card. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Blight(1))
        effect = EffectPatterns.rummage(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Filip Burburan"
        flavorText = "\"Crunch and crack! Gnaw and snap! Be the snacker or be the snack!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4164af6-356e-4de2-8377-dbe70434a996.jpg?1767862542"
    }
}
