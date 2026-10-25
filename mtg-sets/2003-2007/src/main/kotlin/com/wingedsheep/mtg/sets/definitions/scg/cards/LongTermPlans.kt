package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

val LongTermPlans = card("Long-Term Plans") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Search your library for a card, then shuffle and put that card third from the top."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                search = true
            )
            val found = chooseExactly(1, from = searchable)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            toLibraryBottom(found, order = CardOrder.Preserve)
            val aboveCards = gather(CardSource.TopOfLibrary(2))
            toLibraryTop(found, order = CardOrder.Preserve)
            toLibraryTop(aboveCards, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Ben Thompson"
        flavorText = "\"Wait, it'll come to me in a minute.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0422d9-9694-45b6-9c2b-2ca31198cebf.jpg?1562531196"
        ruling("2004-10-04", "If there are fewer than 3 cards in your library, put the card on the bottom of your library.")
    }
}
