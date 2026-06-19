package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Demonic Counsel
 * {1}{B}
 * Sorcery
 * Search your library for a Demon card, reveal it, put it into your hand, then shuffle.
 * Delirium — If there are four or more card types among cards in your graveyard, instead search
 * your library for any card, put it into your hand, then shuffle.
 */
val DemonicCounsel = card("Demonic Counsel") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a Demon card, reveal it, put it into your hand, then shuffle.\nDelirium — If there are four or more card types among cards in your graveyard, instead search your library for any card, put it into your hand, then shuffle."
    spell {
        effect = ConditionalEffect(
            condition = Conditions.Delirium(4),
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.HAND
            ),
            elseEffect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.withSubtype("Demon"),
                destination = SearchDestination.HAND,
                reveal = true
            )
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Babs Webb"
        flavorText = "Valgavoth whispered into Marina's ear until he was all she could hear."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff79c845-4115-4fbf-b20f-37470f2bf7fb.jpg?1726286193"
    }
}
