package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jolly Gerbils
 * {1}{W}
 * Creature — Hamster Citizen
 * 2/3
 *
 * Whenever you give a gift, draw a card.
 */
val JollyGerbils = card("Jolly Gerbils") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Hamster Citizen"
    power = 2
    toughness = 3
    oracleText = "Whenever you give a gift, draw a card."

    triggeredAbility {
        trigger = Triggers.YouGiveAGift
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Manuel Castañón"
        flavorText = "\"With all the chaos going around, we thought it might be the perfect time to take a break and have a nice picnic. Would you like to join us?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eab51d6-ba17-4a8c-8834-25db363f2b6b.jpg?1721425868"
    }
}
