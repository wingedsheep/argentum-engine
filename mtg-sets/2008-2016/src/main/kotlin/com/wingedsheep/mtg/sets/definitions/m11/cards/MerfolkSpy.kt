package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Merfolk Spy — Magic 2011 #66 (canonical printing; reprinted in Magic 2014)
 * {U} · Creature — Merfolk Rogue · 1/1
 *
 * Islandwalk
 * Whenever this creature deals combat damage to a player, that player reveals a card at random
 * from their hand.
 *
 * The damaged player is the combat-damage trigger's [Player.TriggeringPlayer]; the reveal is the
 * Hired Torturer random-reveal pipeline over that player's hand. An empty hand reveals nothing.
 */
val MerfolkSpy = card("Merfolk Spy") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    power = 1
    toughness = 1
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "Whenever this creature deals combat damage to a player, that player reveals a card at random from their hand."

    keywords(Keyword.ISLANDWALK)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer))
            val revealed = chooseRandom(1, from = hand)
            reveal(revealed)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Matt Cavotta & Richard Whitters"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ae05cc-116b-4268-ba78-709aeff36ab1.jpg?1783941823"
        ruling("2010-08-15", "Islandwalk cares about lands with the land type Island, not necessarily lands named Island.")
        ruling(
            "2010-08-15",
            "The second ability triggers just once each time Merfolk Spy deals combat damage to a player, " +
                "regardless of how much damage it deals."
        )
        ruling("2010-08-15", "After the second ability resolves, the revealed card stops being revealed in its owner's hand.")
        ruling(
            "2010-08-15",
            "The next time Merfolk Spy deals combat damage to the same player, the card that's revealed at random " +
                "may be the same card or may be a different card."
        )
    }
}
