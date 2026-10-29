package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Ninja of the Deep Hours — Betrayers of Kamigawa #44 (canonical printing)
 * {3}{U} · Creature — Human Ninja · 2/2
 *
 * Ninjutsu {1}{U}
 * Whenever this creature deals combat damage to a player, you may draw a card.
 */
val NinjaOfTheDeepHours = card("Ninja of the Deep Hours") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 2
    toughness = 2
    oracleText = "Ninjutsu {1}{U} ({1}{U}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, you may draw a card."

    ninjutsu("{1}{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/367a67c7-54db-4336-b55a-3fa27625172a.jpg?1783944206"
        ruling("2021-03-19", "The creature put onto the battlefield with ninjutsu enters the battlefield attacking the same player or planeswalker that the returned creature was attacking. This is a rule specific to ninjutsu.")
        ruling("2021-03-19", "Although the Ninja is attacking, it was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
    }
}
