package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Viashino Racketeer — Return to Ravnica #112
 * {2}{R} · Creature — Lizard Rogue · Common · 2/1
 *
 * When this creature enters, you may discard a card. If you do, draw a card.
 */
val ViashinoRacketeer = card("Viashino Racketeer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Rogue"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Slawomir Maniak"
        flavorText = "\"You may call me a fool, selling this stuff on Orzhov turf. " +
            "Well, you need to be a little reckless to survive.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf4c2d22-9c36-42cc-854d-f96410bb5cf1.jpg?1783940352"
    }
}
