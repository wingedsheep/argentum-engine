package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Moment of Truth — March of the Machine #67
 * {1}{U} · Instant
 *
 * Look at the top three cards of your library. Put one of those cards into your hand, one into
 * your graveyard, and one on the bottom of your library.
 *
 * Same shape as Telling Time: one gather feeding two selections. The remainder of the second
 * selection is the bottom card, so every looked-at card is moved explicitly; a shorter library
 * just leaves the later moves as no-ops.
 */
val MomentOfTruth = card("Moment of Truth") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of your library. Put one of those cards into your " +
        "hand, one into your graveyard, and one on the bottom of your library."

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
            val (toGraveyardCards, toBottom) = chooseExactlySplit(
                1,
                from = notTaken,
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Put on the bottom of your library"
            )
            toGraveyard(toGraveyardCards)
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Rovina Cai"
        flavorText = "On the precipice of eternity, Elspeth made a choice. The fight would not end without her."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e758594-a48c-4508-b400-9028aec07f63.jpg?1783917033"
    }
}
