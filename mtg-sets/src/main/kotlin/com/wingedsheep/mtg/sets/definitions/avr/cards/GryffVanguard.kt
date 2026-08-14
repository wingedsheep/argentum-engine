package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gryff Vanguard
 * {4}{U}
 * Creature — Human Knight
 * 3/2
 * Flying
 * When this creature enters, draw a card.
 */
val GryffVanguard = card("Gryff Vanguard") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Knight"
    oracleText = "Flying\nWhen this creature enters, draw a card."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Jason Chan"
        flavorText =
            "\"Ghouls smashed the door, but I heard the call of the gryffs and knew we were saved.\"\n—Ekka, shopkeeper of Hanweir"
        imageUri =
            "https://cards.scryfall.io/normal/front/b/7/b7238136-c8de-4949-9b54-ff75094e0569.jpg?1783940717"
    }
}
