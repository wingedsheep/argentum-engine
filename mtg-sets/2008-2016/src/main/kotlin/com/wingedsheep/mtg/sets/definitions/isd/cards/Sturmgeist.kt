package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sturmgeist
 * {3}{U}{U}
 * Creature — Spirit
 * * / *
 * Flying
 * Sturmgeist's power and toughness are each equal to the number of cards in your hand.
 * Whenever this creature deals combat damage to a player, draw a card.
 */
val Sturmgeist = card("Sturmgeist") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\nSturmgeist's power and toughness are each equal to the number of cards in your hand.\nWhenever this creature deals combat damage to a player, draw a card."
    keywords(Keyword.FLYING)
    dynamicStats(DynamicAmounts.cardsInYourHand())
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c409d1d0-fc45-40bf-adac-83b680209a38.jpg"
    }
}
