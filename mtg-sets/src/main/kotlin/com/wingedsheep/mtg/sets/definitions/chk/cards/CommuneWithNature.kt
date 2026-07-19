package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Commune with Nature
 * {G}
 * Sorcery
 *
 * Look at the top five cards of your library. You may reveal a creature card from among them and
 * put it into your hand. Put the rest on the bottom of your library in any order.
 *
 * Canonical printing: Champions of Kamigawa (2004). Reprinted in 10E, MM2 and Wilds of Eldraine.
 *
 * "You may reveal" → `chooseUpTo(1)`, so declining is legal even with creature cards among the
 * five. "In any order" → the controller orders the remainder as it goes to the bottom
 * ([com.wingedsheep.sdk.scripting.effects.CardOrder.ControllerChooses], the `toLibraryBottom`
 * default), unlike the later "random order" templating on cards such as Memorial to Unity.
 */
val CommuneWithNature = card("Commune with Nature") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a creature card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(5)), name = "looked")
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true,
                name = "kept",
                remainderName = "rest"
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce0b706e-017d-4f82-b280-cf9fdf75aef8.jpg?1783944291"
    }
}
