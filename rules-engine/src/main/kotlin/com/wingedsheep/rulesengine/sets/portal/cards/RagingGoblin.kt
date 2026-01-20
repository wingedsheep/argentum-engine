package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RagingGoblin {
    val definition = CardDefinition.creature(
        name = "Raging Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.of("Berserker")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.HASTE),
        oracleText = "Haste",
        metadata = ScryfallMetadata(
            collectorNumber = "145",
            rarity = Rarity.COMMON,
            artist = "Pete Venters",
            flavorText = "Charging alone takes uncommon daring or uncommon stupidity. Or both.",
            imageUri = "https://cards.scryfall.io/normal/front/f/e/fed57a17-7847-4e60-bc40-4452880f12a3.jpg",
            scryfallId = "fed57a17-7847-4e60-bc40-4452880f12a3",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Raging Goblin") {
        keywords(Keyword.HASTE)
    }
}
