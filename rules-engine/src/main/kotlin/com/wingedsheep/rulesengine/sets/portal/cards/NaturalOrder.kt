package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost

object NaturalOrder {
    val definition = CardDefinition.sorcery(
        name = "Natural Order",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        oracleText = "As an additional cost to cast this spell, sacrifice a green creature.\n" +
                "Search your library for a green creature card and put it onto the battlefield. " +
                "Then shuffle your library."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "175",
            rarity = Rarity.RARE,
            artist = "Alan Rabinowitz",
            imageUri = "https://cards.scryfall.io/normal/front/c/e/cecb34f8-6961-4c27-9368-26d156714d7b.jpg",
            scryfallId = "cecb34f8-6961-4c27-9368-26d156714d7b",
            releaseDate = "1997-05-01"
        )
    )

    // Filter for green creatures
    private val greenCreatureFilter = CardFilter.And(listOf(
        CardFilter.CreatureCard,
        CardFilter.HasColor(Color.GREEN)
    ))

    val script = cardScript("Natural Order") {
        // Additional cost: sacrifice a green creature
        sacrificeCost(greenCreatureFilter)

        // Effect: search for a green creature and put it onto battlefield
        spell(
            SearchLibraryEffect(
                filter = greenCreatureFilter,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = true
            )
        )
    }
}
