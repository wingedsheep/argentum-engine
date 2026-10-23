package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Harness Infinity — Strixhaven: School of Mages #191
 * {1}{B}{B}{B}{G}{G}{G} · Instant
 *
 * Exchange your hand and graveyard.
 * Exile Harness Infinity.
 *
 * Both zones are gathered before either moves, so the cards that leave the hand are not swept back
 * up by the graveyard half: the hand goes to the graveyard (a plain move, not a discard — no
 * discard triggers), and what was in the graveyard goes to the hand. Either side may be empty.
 * The spell itself is still on the stack throughout and exiles itself on resolution ([selfExile]).
 */
val HarnessInfinity = card("Harness Infinity") {
    manaCost = "{1}{B}{B}{B}{G}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Exchange your hand and graveyard.\nExile Harness Infinity."

    spell {
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            toGraveyard(hand, Player.You)
            toHand(graveyard, Player.You)
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "Seb McKinnon"
        flavorText = "\"As I stepped into the star arch, it all became perfectly clear. Life and death are not " +
            "opposites, but an endless path, twisting around and back upon itself, never ending and never " +
            "beginning.\"\n—Zhaer, professor of biophilosophy"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f74120ce-9f11-449f-bb72-04fe2a27a9f6.jpg?1783927311"
        ruling(
            "2021-04-16",
            "Cards put into your graveyard this way are not discarded. Any ability that triggers when you " +
                "discard a card will not trigger.",
        )
        ruling(
            "2021-04-16",
            "If either your graveyard or your hand has no cards in it, the exchange still happens. If one of " +
                "those zones has any cards in it, you will move them to the other zone.",
        )
    }
}
