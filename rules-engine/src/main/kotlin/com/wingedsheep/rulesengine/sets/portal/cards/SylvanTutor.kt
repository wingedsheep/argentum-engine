package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object SylvanTutor {
    val definition = CardDefinition.sorcery(
        name = "Sylvan Tutor",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Search your library for a creature card, reveal that card, shuffle, then put the card on top of your library."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "188",
            rarity = Rarity.RARE,
            artist = "Kaja Foglio",
            imageUri = "https://cards.scryfall.io/normal/front/2/8/287ba07e-6434-4850-940f-454fcab3f535.jpg",
            scryfallId = "287ba07e-6434-4850-940f-454fcab3f535",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Sylvan Tutor") {
        spell(
            SearchLibraryEffect(
                filter = CardFilter.CreatureCard,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                shuffleAfter = true,
                reveal = true
            )
        )
    }
}
