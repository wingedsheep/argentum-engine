package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ringsight
 * {1}{U}{B}
 * Sorcery
 *
 * The Ring tempts you. Search your library for a card that shares a color with a legendary
 * creature you control, reveal it, put it into your hand, then shuffle.
 *
 * The tutor filter uses the new `sharingColorWithPermanentYouControl(Creature.legendary())`
 * (CardPredicate.SharesColorWithPermanentYouControl). If you control no legendary creature,
 * the filter matches nothing and the search finds nothing.
 */
val Ringsight = card("Ringsight") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "The Ring tempts you. Search your library for a card that shares a color with a legendary " +
        "creature you control, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Effects.TheRingTemptsYou() then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.sharingColorWithPermanentYouControl(
                GameObjectFilter.Creature.legendary()
            ),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Campbell White"
        flavorText = "Frodo was able to see beneath their black wrappings. In their white faces burned keen and merciless eyes."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3700a65c-6f54-4d56-9c6f-8364c45a058c.jpg?1686969951"
    }
}
