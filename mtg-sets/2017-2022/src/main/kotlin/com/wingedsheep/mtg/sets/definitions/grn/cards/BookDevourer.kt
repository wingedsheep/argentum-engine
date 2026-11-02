package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Book Devourer
 * {5}{R}
 * Creature — Beast
 * 4/5
 * Trample
 * Whenever this creature deals combat damage to a player, you may discard all the cards in your
 * hand. If you do, draw that many cards.
 *
 * The "you may … If you do" is the optional trigger: declining skips both halves. Accepting runs
 * Tolarian Winds' discard-then-draw pipeline — the hand is gathered on resolution, discarded, and
 * the draw counts the discarded collection, so "that many" is the hand size at that moment.
 */
val BookDevourer = card("Book Devourer") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, you may discard all the cards in " +
        "your hand. If you do, draw that many cards."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        optional = true
        effect = Effects.Pipeline {
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Kev Walker"
        flavorText = "\"Not my first edition of *The Advokist and the Urchin*!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01dfe640-5bd2-4d0b-8977-887b2ed4c2dd.jpg?1783934166"
    }
}
