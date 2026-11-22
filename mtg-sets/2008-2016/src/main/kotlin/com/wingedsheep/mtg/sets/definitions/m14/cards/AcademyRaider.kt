package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Academy Raider
 * {2}{R}
 * Creature — Human Warrior
 * 1/1
 * Intimidate
 * Whenever this creature deals combat damage to a player, you may discard a card. If you do, draw a card.
 */
val AcademyRaider = card("Academy Raider") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)\n" +
        "Whenever this creature deals combat damage to a player, you may discard a card. If you do, draw a card."

    keywords(Keyword.INTIMIDATE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6652ed29-ee90-4abc-a6cf-6b18a6cbae86.jpg?1783939917"
    }
}
