package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Plundering Predator
 * {4}{R}
 * Creature — Dragon
 * 3/3
 * Flying
 * When this creature enters, you may discard a card. If you do, draw a card.
 */
val PlunderingPredator = card("Plundering Predator") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nWhen this creature enters, you may discard a card. If you do, draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Lucas Graciano"
        flavorText = "You can gather a tidy fortune if you can avoid getting clobbered by falling treasure."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f48b3f46-0e62-4f44-8064-857cd3040659.jpg"
    }
}
