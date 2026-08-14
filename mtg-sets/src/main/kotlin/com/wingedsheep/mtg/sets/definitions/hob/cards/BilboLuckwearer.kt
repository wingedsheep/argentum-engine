package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Bilbo, Luckwearer // Burglar's Plot — The Hobbit #32
 * {1}{U} · Legendary Creature — Halfling Rogue · Uncommon
 * 1/1
 *
 * Bilbo can't be blocked.
 * Whenever Bilbo deals combat damage to a player, draw a card, then discard a card.
 *
 * Adventure: Burglar's Plot — {4}{U}, Sorcery — Adventure
 * Exchange control of two target nonland permanents that share a card type.
 *
 * Modeling notes:
 *  - "Draw a card, then discard a card" is the loot composition, not two loose effects — the
 *    ordering matters (the drawn card is a legal discard), and [Patterns.Hand.loot] encodes it.
 *  - The Adventure's "two target nonland permanents that share a card type" is a *single*
 *    two-target requirement carrying [TargetObject.sameCardType], the card-type sibling of Secret
 *    Tunnel's `sameCreatureType`. Declaring two separate requirements would make the two halves
 *    independent instances of the word "target" and lose the cross-target constraint entirely;
 *    `TargetValidator` enforces the shared card type over the projected types of the chosen pair.
 *  - Neither target is restricted by controller: the printed text says only "two target nonland
 *    permanents", so exchanging two permanents you already control (a legal, pointless play) and
 *    swapping between two *opponents* in a multiplayer game are both allowed.
 *  - (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets its
 *    caster cast the creature later from exile.)
 */
val BilboLuckwearer = card("Bilbo, Luckwearer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Halfling Rogue"
    power = 1
    toughness = 1
    oracleText = "Bilbo can't be blocked.\n" +
        "Whenever Bilbo deals combat damage to a player, draw a card, then discard a card."

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
        description = "Draw a card, then discard a card."
    }

    adventure("Burglar's Plot") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Exchange control of two target nonland permanents that share a card type. " +
            "(Then exile this card. You may cast the creature later from exile.)"

        spell {
            target(
                "two target nonland permanents that share a card type",
                TargetPermanent(
                    count = 2,
                    filter = TargetFilter.NonlandPermanent,
                    sameCardType = true
                )
            )
            effect = Effects.ExchangeControl()
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bff0aa6-16d9-4c83-b598-ef00a3b33d2c.jpg?1783902786"
    }
}
