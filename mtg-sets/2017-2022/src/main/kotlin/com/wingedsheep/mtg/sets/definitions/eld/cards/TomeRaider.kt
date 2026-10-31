package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tome Raider
 * {2}{U}
 * Creature — Faerie
 * 1/1
 * Flying
 * When this creature enters, draw a card.
 */
val TomeRaider = card("Tome Raider") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    oracleText = "Flying\nWhen this creature enters, draw a card."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Randy Vargas"
        flavorText = "\"Humans are so forgetful. Every page I steal becomes a secret they can't remember.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e04ad850-5801-4654-a388-f86be20a43bf.jpg"
    }
}
