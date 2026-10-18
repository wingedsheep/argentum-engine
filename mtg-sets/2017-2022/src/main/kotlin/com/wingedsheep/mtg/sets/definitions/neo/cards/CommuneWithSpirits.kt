package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Commune with Spirits
 * {G}
 * Sorcery
 *
 * Look at the top four cards of your library. You may reveal an enchantment or land card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 */
val CommuneWithSpirits = card("Commune with Spirits") {
    manaCost = "{G}"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. You may reveal an enchantment or land card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Enchantment or GameObjectFilter.Land,
                prompt = "You may reveal an enchantment or land card and put it into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Aurore Folny"
        flavorText = "The kami of Jukai Forest accept only the most respectful petitioners."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a35ff4dd-bad3-4496-82b3-8253f48af06d.jpg?1783923852"
    }
}
