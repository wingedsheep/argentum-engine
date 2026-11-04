package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Mastermind's Acquisition {2}{B}{B}
 * Sorcery
 *
 * Choose one —
 * • Search your library for a card, put it into your hand, then shuffle.
 * • Put a card you own from outside the game into your hand.
 *
 * Mode one is Demonic Tutor ([Patterns.Library.searchLibrary]). Mode two is a wish over the
 * private sideboard zone ([Patterns.Sideboard.wish]); unlike Burning Wish it has no "may" and no
 * "reveal", so it is `optional = false, revealed = false` — the same shape as Ring of Ma'rûf.
 */
val MastermindsAcquisition = card("Mastermind's Acquisition") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for a card, put it into your hand, then shuffle.\n" +
        "• Put a card you own from outside the game into your hand."

    spell {
        modal {
            mode("Search your library for a card, put it into your hand, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Any,
                    destination = SearchDestination.HAND
                )
            }
            mode("Put a card you own from outside the game into your hand") {
                effect = Patterns.Sideboard.wish(GameObjectFilter.Any, revealed = false, optional = false)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Svetlin Velinov"
        flavorText = "Another piece of a nefarious puzzle."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f00f09b-a113-460e-ac2f-d3988a83179e.jpg?1783935310"
        ruling(
            "2018-01-19",
            "In a casual game, a card you choose from outside the game comes from your personal " +
                "collection. In a tournament event, a card you choose from outside the game must come " +
                "from your sideboard. You may look at your sideboard at any time."
        )
    }
}
