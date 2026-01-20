package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object StormCrow {
    val definition = CardDefinition.creature(
        name = "Storm Crow",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.BIRD),
        power = 1,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "69",
            rarity = Rarity.COMMON,
            artist = "Una Fricker",
            flavorText = "Storm crow descending, winter unending. Storm crow departing, summer is starting.",
            imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe87b59-b456-4532-a695-0dea3110d878.jpg",
            scryfallId = "dfe87b59-b456-4532-a695-0dea3110d878",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Storm Crow") {
        keywords(Keyword.FLYING)
    }
}
