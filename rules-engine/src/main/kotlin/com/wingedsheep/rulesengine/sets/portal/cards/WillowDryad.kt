package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WillowDryad {
    val definition = CardDefinition.creature(
        name = "Willow Dryad",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.DRYAD),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FORESTWALK),
        oracleText = "Forestwalk"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "193",
            rarity = Rarity.COMMON,
            artist = "D. Alexander Gregory",
            flavorText = "Some think that dryads are the dreams of trees.",
            imageUri = "https://cards.scryfall.io/normal/front/1/d/1def835b-aabb-4a9d-8fb9-460452de0d79.jpg",
            scryfallId = "1def835b-aabb-4a9d-8fb9-460452de0d79",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Willow Dryad") {
        keywords(Keyword.FORESTWALK)
    }
}
