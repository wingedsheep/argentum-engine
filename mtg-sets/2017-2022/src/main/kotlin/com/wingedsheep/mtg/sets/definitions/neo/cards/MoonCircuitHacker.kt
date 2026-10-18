package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Moon-Circuit Hacker — Kamigawa: Neon Dynasty #67 (canonical printing)
 * {1}{U} · Enchantment Creature — Human Ninja · 2/1
 *
 * Ninjutsu {U}
 * Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard
 * a card unless this creature entered this turn.
 *
 * "Entered this turn" reads last-known information when the Ninja has left the battlefield before
 * the trigger resolves (CR 608.2h) — bouncing it in response doesn't turn the draw into a loot.
 */
val MoonCircuitHacker = card("Moon-Circuit Hacker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Human Ninja"
    power = 2
    toughness = 1
    oracleText = "Ninjutsu {U} ({U}, Return an unblocked attacker you control to hand: Put this card onto " +
        "the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard " +
        "a card unless this creature entered this turn."

    ninjutsu("{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(
            Effects.DrawCards(1) then
                Effects.If(Conditions.Not(Conditions.SourceEnteredThisTurn), Effects.Discard(1))
        )
        description = "Whenever this creature deals combat damage to a player, you may draw a card. " +
            "If you do, discard a card unless this creature entered this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Tia Masic"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6e466d1-943d-41e6-a47d-c9d951ca4262.jpg?1790210418"
    }
}
