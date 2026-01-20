package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object DjinnOfTheLamp {
    val definition = CardDefinition.creature(
        name = "Djinn of the Lamp",
        manaCost = ManaCost.parse("{5}{U}{U}"),
        subtypes = setOf(Subtype.DJINN),
        power = 5,
        toughness = 6,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "52",
            rarity = Rarity.RARE,
            artist = "DiTerlizzi",
            flavorText = "Once they learn the trick of carrying their own lamps, they never touch the ground again.",
            imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5e7b52-2663-4140-9758-f24b8b947876.jpg",
            scryfallId = "3a5e7b52-2663-4140-9758-f24b8b947876",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Djinn of the Lamp") {
        keywords(Keyword.FLYING)
    }
}
