package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Park Heights Pegasus
 * {G}{W}
 * Creature — Pegasus
 * 2/1
 * Flying, trample
 * Whenever this creature deals combat damage to a player, draw a card if you had two or more
 * creatures enter the battlefield under your control this turn.
 *
 * The "if" trails the effect, so it is a resolution-time check rather than an intervening-'if':
 * the ability always triggers and [Effects.If] reads the turn's creature-entry tracker
 * ([Conditions.CreaturesEnteredThisTurn]) as it resolves. A past-event count — the creatures
 * needn't still be on the battlefield.
 */
val ParkHeightsPegasus = card("Park Heights Pegasus") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Pegasus"
    power = 2
    toughness = 1
    oracleText = "Flying, trample\nWhenever this creature deals combat damage to a player, draw a " +
        "card if you had two or more creatures enter the battlefield under your control this turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.If(
            condition = Conditions.CreaturesEnteredThisTurn(atLeast = 2),
            then = Effects.DrawCards(1),
        )
        description = "Whenever this creature deals combat damage to a player, draw a card if you " +
            "had two or more creatures enter the battlefield under your control this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Randy Gallegos"
        flavorText = "The burglar turned, bowed, and dropped from the ledge onto her waiting mount."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bba250e8-1ceb-40f5-95f1-fb00a2a723eb.jpg?1783923073"
    }
}
