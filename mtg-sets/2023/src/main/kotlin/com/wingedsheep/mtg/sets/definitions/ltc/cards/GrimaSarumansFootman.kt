package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gríma, Saruman's Footman
 * {2}{U}{B}
 * Legendary Creature — Human Advisor
 * 1/4
 *
 * Gríma can't be blocked.
 * Whenever Gríma deals combat damage to a player, that player exiles cards from the top of their
 * library until they exile an instant or sorcery card. You may cast that card without paying its
 * mana cost. Then that player puts the exiled cards that weren't cast this way on the bottom of
 * their library in a random order.
 *
 * Chaos Wand's "exile until, may cast free, rest to the bottom at random" pipeline, walked over the
 * library of the player Gríma hit ([Player.TriggeringPlayer]) rather than a targeted opponent. The
 * cast happens during resolution, by Gríma's controller; the cast card has left exile, so only the
 * cards still in exile go back to the bottom of that player's library.
 */
val GrimaSarumansFootman = card("Gríma, Saruman's Footman") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 4
    oracleText = "Gríma can't be blocked.\n" +
        "Whenever Gríma deals combat damage to a player, that player exiles cards from the top of " +
        "their library until they exile an instant or sorcery card. You may cast that card without " +
        "paying its mana cost. Then that player puts the exiled cards that weren't cast this way on " +
        "the bottom of their library in a random order."

    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val (found, exiled) = gatherUntilMatch(GameObjectFilter.InstantOrSorcery, player = Player.TriggeringPlayer)
            exile(exiled)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
            val notCast = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toLibraryBottom(notCast, player = Player.TriggeringPlayer, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00f40514-d37d-423f-aa0c-1875300f43a8.jpg?1783916020"
        ruling(
            "2023-06-16",
            "If you choose not to cast the card, it is put on the bottom of its owner's library in a random order along with the other exiled cards."
        )
        ruling(
            "2023-06-16",
            "If you cast a spell \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, you must pay those."
        )
        ruling(
            "2023-06-16",
            "If that player exiles their entire library without exiling an instant or sorcery card, they will randomize the order of the exiled cards, and the cards then become that player's library, ending the effect. They will not continue to exile and randomize their library forever."
        )
        ruling(
            "2023-06-16",
            "You cast the card while the ability is resolving and still on the stack. You can't wait to cast it later in the turn."
        )
    }
}
