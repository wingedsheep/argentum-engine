package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Loch Dragon
 * {U/R}{U/R}{U/R}{U/R}
 * Creature — Dragon
 * 3/2
 *
 * Flying
 * Whenever this creature enters or attacks, you may discard a card. If you do, draw a card.
 */
val LochDragon = card("Loch Dragon") {
    manaCost = "{U/R}{U/R}{U/R}{U/R}"
    colorIdentity = "RU"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\n" +
        "Whenever this creature enters or attacks, you may discard a card. If you do, draw a card."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Mike Bierek"
        flavorText = "Despite its ferocity, its favor can be won with a gift of something it's never seen before."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a99088e-7162-4e65-867f-bca1c7400ce3.jpg?1783932589"
    }
}
