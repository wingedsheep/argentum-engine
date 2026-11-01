package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity

/**
 * Burning-Tree Vandal — Ravnica Allegiance #94
 * {2}{R} · Creature — Human Rogue · 2 / 1
 */
val BurningTreeVandal = card("Burning-Tree Vandal") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "Whenever this creature attacks, you may discard a card. If you do, draw a card."

    riot()

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Aaron Miller"
        flavorText = "The ostentatious splendor of the Orzhov church was just asking to be smashed."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5674753-17e4-4e35-a12c-13e1e077ec70.jpg?1783933685"
    }
}
