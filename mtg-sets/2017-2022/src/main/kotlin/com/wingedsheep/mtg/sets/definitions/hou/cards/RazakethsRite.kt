package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val RazakethsRite = card("Razaketh's Rite") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card, put that card into your hand, then shuffle.\n" +
        "Cycling {B} ({B}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND)
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Izzy"
        flavorText = "\"The blood ritual is complete. The end approaches.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd36987a-48c3-477b-b859-0268e84b7e7b.jpg?1783936036"
    }
}
