package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object HornedTurtle {
    val definition = CardDefinition.creature(
        name = "Horned Turtle",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.TURTLE),
        power = 1,
        toughness = 4,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "57",
            rarity = Rarity.COMMON,
            artist = "Adrian Smith",
            flavorText = "Its spear has no shaft, its shield no handle.",
            imageUri = "https://cards.scryfall.io/normal/front/a/7/a7d25497-36b4-48b9-ba01-f24f6222d6be.jpg",
            scryfallId = "a7d25497-36b4-48b9-ba01-f24f6222d6be",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Horned Turtle") {
        // Vanilla creature - no abilities
    }
}
