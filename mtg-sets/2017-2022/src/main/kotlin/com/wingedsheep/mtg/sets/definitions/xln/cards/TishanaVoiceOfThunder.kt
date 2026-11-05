package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize

/**
 * Tishana, Voice of Thunder — Ixalan #230 (canonical printing)
 * {5}{G}{U} · Legendary Creature — Merfolk Shaman · * / *
 *
 * Tishana's power and toughness are each equal to the number of cards in your hand.
 * You have no maximum hand size.
 * When Tishana enters, draw a card for each creature you control.
 *
 * The draw count is read at resolution and includes Tishana herself.
 */
val TishanaVoiceOfThunder = card("Tishana, Voice of Thunder") {
    manaCost = "{5}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Shaman"
    oracleText = "Tishana's power and toughness are each equal to the number of cards in your hand.\n" +
        "You have no maximum hand size.\n" +
        "When Tishana enters, draw a card for each creature you control."

    dynamicPower(DynamicAmounts.cardsInYourHand())
    dynamicToughness(DynamicAmounts.cardsInYourHand())

    staticAbility { ability = NoMaximumHandSize() }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19c5cfbe-3972-4d2e-a37f-e96cca33bb33.jpg?1783935708"
    }
}
