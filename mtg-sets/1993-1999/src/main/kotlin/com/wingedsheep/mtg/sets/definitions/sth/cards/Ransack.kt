package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/** The caster looks, chooses, and orders both piles, even when targeting an opponent. */
val Ransack = card("Ransack") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of target player's library. Put any number of them on " +
        "the bottom of that library in any order and the rest on top of the library in any order."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5, player.asPlayer))
            val (bottom, top) = chooseUpToSplit(
                5,
                from = looked,
                selectedLabel = "Put on the bottom of that library",
                remainderLabel = "Put back on top of that library"
            )
            toLibraryBottom(bottom, player.asPlayer, order = CardOrder.ControllerChooses)
            toLibraryTop(top, player.asPlayer, order = CardOrder.ControllerChooses)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b438802b-629a-42c8-824d-f081a1619f68.jpg?1783946567"
    }
}
