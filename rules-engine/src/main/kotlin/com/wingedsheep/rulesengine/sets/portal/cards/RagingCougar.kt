package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RagingCougar {
    val definition = CardDefinition.creature(
        name = "Raging Cougar",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.CAT),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.HASTE),
        oracleText = "Haste"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "144",
            rarity = Rarity.COMMON,
            artist = "Terese Nielsen",
            flavorText = "Mountaineers quickly learn to travel with their spears always pointed up.",
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd9d126a-9db9-4adc-9cf6-11408c63201d.jpg",
            scryfallId = "fd9d126a-9db9-4adc-9cf6-11408c63201d",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Raging Cougar") {
        keywords(Keyword.HASTE)
    }
}
