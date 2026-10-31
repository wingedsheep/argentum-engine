package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cloudkin Seer (M20 #54)
 * {2}{U}  Creature — Elemental Wizard  2/1
 *
 * Flying
 * When this creature enters, draw a card.
 */
val CloudkinSeer = card("Cloudkin Seer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Wizard"
    oracleText = "Flying\n" +
        "When this creature enters, draw a card."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"I can see which way the wind is blowing.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2111753-a930-403f-9d94-a86dfcb069da.jpg"
    }
}
