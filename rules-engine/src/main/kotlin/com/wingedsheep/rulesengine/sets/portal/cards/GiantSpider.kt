package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object GiantSpider {
    val definition = CardDefinition.creature(
        name = "Giant Spider",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype.of("Spider")),
        power = 2,
        toughness = 4,
        keywords = setOf(Keyword.REACH),
        oracleText = "Reach",
        metadata = ScryfallMetadata(
            collectorNumber = "167",
            rarity = Rarity.COMMON,
            artist = "Randy Gallegos",
            flavorText = "It's not far into the trap, as the crow flies.",
            imageUri = "https://cards.scryfall.io/normal/front/2/9/2995530c-16bd-4dcb-99c2-008bba00052c.jpg",
            scryfallId = "2995530c-16bd-4dcb-99c2-008bba00052c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Giant Spider") {
        keywords(Keyword.REACH)
    }
}
