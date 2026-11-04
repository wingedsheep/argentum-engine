package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val BeneathTheSands = card("Beneath the Sands") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Magali Villeneuve"
        flavorText = "Though buried and muffled, nature's whisper still called to Nissa."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e92c0cf0-df4a-4e50-8396-53713f925b53.jpg?1783936022"
    }
}
