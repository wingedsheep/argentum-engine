package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Captain of Umbar
 * {2}{U}
 * Creature — Human Pirate
 * 2/3
 * {1}, {T}: Draw a card, then discard a card.
 */
val CaptainOfUmbar = card("Captain of Umbar") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 3
    oracleText = "{1}, {T}: Draw a card, then discard a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = EffectPatterns.loot(draw = 1, discard = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Wei Guan"
        flavorText = "\"There is a great fleet drawing near to the mouths of Anduin, manned by the Corsairs of Umbar in the South. They have long ceased to fear the might of Gondor.\"\n—Beregond"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c222577d-a3c7-41d9-b11b-62065bdb98ef.jpg?1686968046"
    }
}
