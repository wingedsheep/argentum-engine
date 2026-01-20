package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RedwoodTreefolk {
    val definition = CardDefinition.creature(
        name = "Redwood Treefolk",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(Subtype.TREEFOLK),
        power = 3,
        toughness = 6,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "183",
            rarity = Rarity.COMMON,
            artist = "Steve Luke",
            flavorText = "The soldiers chopped and chopped, and still the great tree stood, frowning down at them.",
            imageUri = "https://cards.scryfall.io/normal/front/e/9/e9399667-ae2a-4b64-84dd-8f97f3e5fe79.jpg",
            scryfallId = "e9399667-ae2a-4b64-84dd-8f97f3e5fe79",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Redwood Treefolk") {
        // Vanilla creature - no abilities
    }
}
