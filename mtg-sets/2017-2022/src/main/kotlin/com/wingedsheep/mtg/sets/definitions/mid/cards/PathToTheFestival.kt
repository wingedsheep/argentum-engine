package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Path to the Festival
 * {2}{G}
 * Sorcery
 * Search your library for a basic land card, put that card onto the battlefield tapped, then
 * shuffle. Then if there are three or more basic land types among lands you control, scry 1.
 * Flashback {4}{G}
 *
 * The domain check runs after the search resolves, so the fetched land counts toward it.
 */
val PathToTheFestival = card("Path to the Festival") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put that card onto the battlefield tapped, " +
        "then shuffle. Then if there are three or more basic land types among lands you control, scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)\n" +
        "Flashback {4}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true,
        ) then Effects.If(
            condition = Conditions.BasicLandTypesAtLeast(3),
            then = Patterns.Library.scry(1),
        )
    }

    keywordAbility(KeywordAbility.flashback("{4}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/153f545d-bdbf-42c7-837a-a7076bbe5027.jpg?1783925576"
    }
}
