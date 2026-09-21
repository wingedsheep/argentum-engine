package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val LoyalTutor = card("Loyal Tutor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Search your library for a planeswalker card, reveal it, then shuffle and put that card on top."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Planeswalker,
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Darren Tan"
        flavorText = "\"We swore to keep watch,\" Ajani growled through gritted teeth, \"and that includes against our own.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/490dae91-94ce-42a9-a11f-6c5e77c4e486.jpg?1788878096"
        inBooster = false
    }
}
