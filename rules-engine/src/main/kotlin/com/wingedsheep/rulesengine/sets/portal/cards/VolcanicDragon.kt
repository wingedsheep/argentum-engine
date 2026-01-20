package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object VolcanicDragon {
    val definition = CardDefinition.creature(
        name = "Volcanic Dragon",
        manaCost = ManaCost.parse("{4}{R}{R}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.FLYING, Keyword.HASTE),
        oracleText = "Flying, haste",
        metadata = ScryfallMetadata(
            collectorNumber = "153",
            rarity = Rarity.RARE,
            artist = "Tom Wänerstrand",
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d99c5c70-7568-42d3-939c-b6ee1ed94b9f.jpg",
            scryfallId = "d99c5c70-7568-42d3-939c-b6ee1ed94b9f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Volcanic Dragon") {
        keywords(Keyword.FLYING, Keyword.HASTE)
    }
}
