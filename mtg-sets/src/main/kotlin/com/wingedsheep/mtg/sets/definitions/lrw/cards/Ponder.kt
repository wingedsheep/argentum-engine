package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect

/**
 * Ponder
 * {U}
 * Sorcery
 * Look at the top three cards of your library, then put them back in any order. You may shuffle.
 * Draw a card.
 *
 * Same shape as Omen (Portal): look-and-reorder pipeline + an optional shuffle + a draw. The
 * "you may shuffle" happens after reordering, so a shuffled library discards the chosen order —
 * see [com.wingedsheep.mtg.sets.definitions.por.cards.Omen].
 */
val Ponder = card("Ponder") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library, then put them back in any order. You may shuffle.\nDraw a card."
    spell {
        effect = Effects.Composite(
            Patterns.Library.lookAtTopAndReorder(count = 3),
            MayEffect(ShuffleLibraryEffect()),
            DrawCardsEffect(1)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Mark Tedin"
        flavorText = "\"We see the same sky as you, just through a different lens.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba6b6fc5-5077-4812-b8e9-906783dbaf67.jpg?1783942899"
    }
}
