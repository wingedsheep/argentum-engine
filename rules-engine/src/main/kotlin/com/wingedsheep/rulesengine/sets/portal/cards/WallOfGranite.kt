package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WallOfGranite {
    val definition = CardDefinition.creature(
        name = "Wall of Granite",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.WALL),
        power = 0,
        toughness = 7,
        keywords = setOf(Keyword.DEFENDER),
        oracleText = "Defender"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "155",
            rarity = Rarity.UNCOMMON,
            artist = "Kev Walker",
            flavorText = "The wisest man builds his house behind the rock.",
            imageUri = "https://cards.scryfall.io/normal/front/7/0/70c5ac71-bf45-4b99-8184-36ce88dd728a.jpg",
            scryfallId = "70c5ac71-bf45-4b99-8184-36ce88dd728a",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Wall of Granite") {
        keywords(Keyword.DEFENDER)
    }
}
