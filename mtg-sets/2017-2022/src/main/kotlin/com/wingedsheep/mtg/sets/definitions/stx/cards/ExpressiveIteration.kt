package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Expressive Iteration — Strixhaven: School of Mages #186
 * {U}{R} · Sorcery
 *
 * Look at the top three cards of your library. Put one of them into your hand, put one of them on
 * the bottom of your library, and exile one of them. You may play the exiled card this turn.
 *
 * Modelling notes:
 * - The Telling Time / Moment of Truth shape: one gather feeding two selections. The second
 *   selection picks the *bottom* card and exiles the remainder, so the instructions run in printed
 *   order — with only two cards in the library, one goes to hand and the other is forced to the
 *   bottom, and nothing is exiled (the printed ruling).
 * - "Play", not "cast": no `nonLandOnly`, so an exiled land is playable with a land drop available.
 *   The grant expires at end of turn; an unplayed card stays exiled.
 */
val ExpressiveIteration = card("Expressive Iteration") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. Put one of them into your hand, put " +
        "one of them on the bottom of your library, and exile one of them. You may play the exiled " +
        "card this turn."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3))
            val (toHandCards, notTaken) = chooseExactlySplit(
                1,
                from = looked,
                selectedLabel = "Put into your hand",
                remainderLabel = "Not put into your hand"
            )
            toHand(toHandCards)
            val (toBottom, exiled) = chooseExactlySplit(
                1,
                from = notTaken,
                selectedLabel = "Put on the bottom of your library",
                remainderLabel = "Exile (you may play it this turn)"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Prismari students dance in the tension between passion and perfection."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31b770cc-09e7-4c0b-b2a4-462ab4f7200d.jpg?1783927314"
        ruling("2026-03-20", "If there are fewer than three cards in your library, follow the instructions in the order given for any cards that are there. For example, if there are two cards in your library, you'll put one of them into your hand and one of them on the bottom of your library. You won't have the option to exile either of them.")
        ruling("2026-03-20", "If you don't play the exiled card, it remains exiled. It won't be available to be played on future turns.")
    }
}
