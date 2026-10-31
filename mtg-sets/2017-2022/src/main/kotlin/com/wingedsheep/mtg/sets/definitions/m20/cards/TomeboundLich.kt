package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Tomebound Lich
 * {1}{U}{B}
 * Creature — Zombie Wizard
 * 1/3
 *
 * Deathtouch
 * Lifelink
 * Whenever this creature enters or deals combat damage to a player, draw a card,
 * then discard a card.
 *
 * The "enters or deals combat damage" trigger is two triggered abilities sharing
 * one effect, as on Kitsune, Dragon's Daughter.
 */
val TomeboundLich = card("Tomebound Lich") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Wizard"
    oracleText = "Deathtouch\nLifelink\nWhenever this creature enters or deals combat damage to a player, draw a card, then discard a card."
    power = 1
    toughness = 3

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
        description = "When this creature enters, draw a card, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
        description = "Whenever this creature deals combat damage to a player, draw a card, then discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f305027-dfd2-4b17-a6af-6b0576389623.jpg?1783932947"
    }
}
