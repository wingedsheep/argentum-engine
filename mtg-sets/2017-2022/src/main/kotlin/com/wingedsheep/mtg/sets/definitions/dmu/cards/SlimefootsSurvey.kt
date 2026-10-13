package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Slimefoot's Survey
 * {4}{G}
 * Sorcery
 * Domain — Search your library for up to two land cards that each have a basic land type, put them
 * onto the battlefield tapped, then shuffle. Look at the top X cards of your library, where X is the
 * number of basic land types among lands you control. Put up to one of them on top of your library
 * and the rest on the bottom of your library in a random order.
 *
 * X is counted when the look happens, after the fetched lands are already on the battlefield, so
 * they contribute to domain. The look-and-sort is Nael, Avizoa Aeronaut's pipeline.
 */
val SlimefootsSurvey = card("Slimefoot's Survey") {
    manaCost = "{4}{G}"
    typeLine = "Sorcery"
    oracleText = "Domain — Search your library for up to two land cards that each have a basic land " +
        "type, put them onto the battlefield tapped, then shuffle. Look at the top X cards of your " +
        "library, where X is the number of basic land types among lands you control. Put up to one of " +
        "them on top of your library and the rest on the bottom of your library in a random order."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.LandWithBasicLandType,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        ) then Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.domain(),
                    player = Player.You
                )
            )
            val split = chooseUpToSplit(
                count = 1,
                from = looked,
                prompt = "Put up to one card on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Put on the bottom in a random order"
            )
            toLibraryTop(split.selected)
            toLibraryBottom(split.remainder, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/622f96fc-179a-4ff6-95a5-de721492f7eb.jpg?1783921296"
    }
}
