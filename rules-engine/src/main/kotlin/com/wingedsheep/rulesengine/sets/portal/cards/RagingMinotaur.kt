package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RagingMinotaur {
    val definition = CardDefinition.creature(
        name = "Raging Minotaur",
        manaCost = ManaCost.parse("{2}{R}{R}"),
        subtypes = setOf(Subtype.MINOTAUR, Subtype.BERSERKER),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.HASTE),
        oracleText = "Haste"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "146",
            rarity = Rarity.COMMON,
            artist = "Scott M. Fischer",
            flavorText = "The only thing worse than a minotaur with an axe is an angry minotaur with an axe.",
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ea4be95-8147-431c-8bb8-8fe7e5a2ad53.jpg",
            scryfallId = "2ea4be95-8147-431c-8bb8-8fe7e5a2ad53",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Raging Minotaur") {
        keywords(Keyword.HASTE)
    }
}
