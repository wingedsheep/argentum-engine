package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object Archangel {
    val definition = CardDefinition.creature(
        name = "Archangel",
        manaCost = ManaCost.parse("{5}{W}{W}"),
        subtypes = setOf(Subtype.ANGEL),
        power = 5,
        toughness = 5,
        keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
        oracleText = "Flying, vigilance",
        metadata = ScryfallMetadata(
            collectorNumber = "3",
            rarity = Rarity.RARE,
            artist = "Quinton Hoover",
            imageUri = "https://cards.scryfall.io/normal/front/3/8/387b9236-1241-44b7-9436-1fbc9970b692.jpg",
            scryfallId = "387b9236-1241-44b7-9436-1fbc9970b692",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Archangel") {
        keywords(Keyword.FLYING, Keyword.VIGILANCE)
    }
}
