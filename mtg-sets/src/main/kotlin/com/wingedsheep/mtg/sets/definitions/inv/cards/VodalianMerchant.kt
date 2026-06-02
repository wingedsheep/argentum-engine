package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Vodalian Merchant
 * {1}{U}
 * Creature — Merfolk
 * 1/2
 * When this creature enters, draw a card, then discard a card.
 */
val VodalianMerchant = card("Vodalian Merchant") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1c0effa-a4b8-4166-a66a-90cf01c6ea0d.jpg?1562933932"
    }
}
