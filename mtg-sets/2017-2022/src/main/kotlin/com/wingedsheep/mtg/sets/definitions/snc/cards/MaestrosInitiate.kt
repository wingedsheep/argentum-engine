package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Maestros Initiate
 * {2}{B}
 * Creature — Human Citizen
 * 3 / 1
 * {4}{U/R}, Exile this card from your graveyard: Draw two cards, then discard a card.
 */
val MaestrosInitiate = card("Maestros Initiate") {
    manaCost = "{2}{B}"
    colorIdentity = "BRU"
    typeLine = "Creature — Human Citizen"
    oracleText = "{4}{U/R}, Exile this card from your graveyard: Draw two cards, then discard a card."
    power = 3
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U/R}"), Costs.ExileSelf)
        effect = Patterns.Hand.loot(draw = 2, discard = 1)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Irina Nordsol"
        flavorText = "\"How far are you willing to go for your family, cleanblade?\"\n—Lord Xander"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db94f2a9-53be-449e-8240-77f013cdd7e3.jpg"
    }
}
