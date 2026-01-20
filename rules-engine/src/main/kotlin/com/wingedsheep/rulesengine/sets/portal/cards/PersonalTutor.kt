package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object PersonalTutor {
    val definition = CardDefinition.sorcery(
        name = "Personal Tutor",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Search your library for a sorcery card, reveal it, then shuffle and put that card on top."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "64",
            rarity = Rarity.UNCOMMON,
            artist = "D. Alexander Gregory",
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1edc3917-fded-4773-8f8d-62bd861c1131.jpg",
            scryfallId = "1edc3917-fded-4773-8f8d-62bd861c1131",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Personal Tutor") {
        spell(
            SearchLibraryEffect(
                filter = CardFilter.SorceryCard,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                shuffleAfter = true,
                reveal = true
            )
        )
    }
}
