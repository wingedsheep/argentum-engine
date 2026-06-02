package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Grow from the Ashes
 * {2}{G}
 * Sorcery
 * Kicker {2}
 * Search your library for a basic land card, put it onto the battlefield, then shuffle.
 * If this spell was kicked, instead search your library for two basic land cards, put them
 * onto the battlefield, then shuffle.
 */
val GrowFromTheAshes = card("Grow from the Ashes") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\nSearch your library for a basic land card, put it onto the battlefield, then shuffle. If this spell was kicked, instead search your library for two basic land cards, put them onto the battlefield, then shuffle."

    keywordAbility(KeywordAbility.kicker("{2}"))

    spell {
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 2,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = true
            ),
            elseEffect = LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51d4d1c2-671c-498c-a232-7d076e3dc3bb.jpg?1562911270"
    }
}