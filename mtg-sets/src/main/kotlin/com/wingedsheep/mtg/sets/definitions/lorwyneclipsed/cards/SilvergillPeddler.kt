package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Silvergill Peddler
 * {2}{U}
 * Creature — Merfolk Citizen
 * 2/3
 *
 * Whenever this creature becomes tapped, draw a card, then discard a card.
 */
val SilvergillPeddler = card("Silvergill Peddler") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Merfolk Citizen"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature becomes tapped, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = EffectPatterns.loot(draw = 1, discard = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "John Tedrick"
        flavorText = "\"I should let you know, Longlake Clachan was very interested in that one, but I'm willing to entertain a better offer.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feba2bb6-6005-4ade-a4c5-97bbd54b43a3.jpg?1767732538"
    }
}
