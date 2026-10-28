package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Adventure Awaits — Zendikar Rising #177
 * {1}{G} · Sorcery
 *
 * Look at the top five cards of your library. You may reveal a creature card from among them and
 * put it into your hand. Put the rest on the bottom of your library in a random order. If you
 * didn't put a card into your hand this way, draw a card.
 *
 * The rest go to the bottom before the conditional draw, so with five or fewer cards left in the
 * library the draw still finds one of the bottomed cards.
 */
val AdventureAwaits = card("Adventure Awaits") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a creature card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a " +
        "random order. If you didn't put a card into your hand this way, draw a card."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card and put it into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toLibraryBottom(rest, order = CardOrder.Random)
            ifNotEmpty(kept) {
                toHand(kept, revealed = true)
            } orElse {
                run(Effects.DrawCards(1))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Billy Christian"
        flavorText = "\"Good, we're all here. Let's get started!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db50f988-da46-4b9b-9cc5-78497df2df8b.jpg?1783929342"
    }
}
