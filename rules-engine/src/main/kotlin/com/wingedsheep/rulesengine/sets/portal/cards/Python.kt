package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object Python {
    val definition = CardDefinition.creature(
        name = "Python",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        subtypes = setOf(Subtype.of("Snake")),
        power = 3,
        toughness = 2,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "105",
            rarity = Rarity.COMMON,
            artist = "Alan Rabinowitz",
            flavorText = "What could it have swallowed to grow so large?",
            imageUri = "https://cards.scryfall.io/normal/front/8/2/82c552a1-6245-4caf-8249-765ce7ea80d2.jpg",
            scryfallId = "82c552a1-6245-4caf-8249-765ce7ea80d2",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Python") {
        // Vanilla creature - no abilities
    }
}
