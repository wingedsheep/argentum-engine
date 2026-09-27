package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Petals of Insight — Champions of Kamigawa #79
 * {4}{U} · Sorcery — Arcane
 *
 * Look at the top three cards of your library. You may put those cards on the bottom of your
 * library in any order. If you do, return Petals of Insight to its owner's hand. Otherwise, draw
 * three cards.
 *
 * - The three looked-at cards move together: the choice is all-or-nothing, so it is a single
 *   `May` over the whole gathered collection rather than a per-card selection.
 * - "If you do" → the return happens *during* resolution: `CardSource.Self` gathers the resolving
 *   spell off the stack and moves it to its **owner's** hand, so the CR 608.2n "put it into its
 *   owner's graveyard" step finds nothing to move (the 2013-06-07 ruling: it never goes to a
 *   graveyard). Same shape as Hanabi Blast.
 * - "Otherwise" covers the decline: the cards stay on top and you draw them.
 */
val PetalsOfInsight = card("Petals of Insight") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Arcane"
    oracleText = "Look at the top three cards of your library. You may put those cards on the bottom of " +
        "your library in any order. If you do, return Petals of Insight to its owner's hand. " +
        "Otherwise, draw three cards."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3))
            run(
                Effects.May(
                    effect = Effects.Pipeline {
                        toLibraryBottom(looked)
                        toHand(gather(CardSource.Self), Player.OwnerOfSource)
                    },
                    otherwise = Effects.DrawCards(3),
                    prompt = "Put the top three cards of your library on the bottom in any order " +
                        "and return Petals of Insight to your hand? (Otherwise, draw three cards.)"
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00f59a16-45a8-4b52-a8df-ea96abafd8ff.jpg?1783944323"
        ruling(
            "2013-06-07",
            "If you choose to put the three cards on the bottom of your library, Petals of Insight " +
                "will return to your hand directly from the stack. It never goes to a graveyard."
        )
    }
}
