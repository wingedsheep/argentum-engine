package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WindDrake {
    val definition = CardDefinition.creature(
        name = "Wind Drake",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.DRAKE),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying",
        metadata = ScryfallMetadata(
            collectorNumber = "77",
            rarity = Rarity.COMMON,
            artist = "Zina Saunders",
            flavorText = "\"No bird soars too high, if he soars with his own wings.\" — William Blake, The Marriage of Heaven and Hell",
            imageUri = "https://cards.scryfall.io/normal/front/5/4/5486d2dc-9a5d-4f58-a5ec-d94de54b852f.jpg",
            scryfallId = "5486d2dc-9a5d-4f58-a5ec-d94de54b852f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Wind Drake") {
        keywords(Keyword.FLYING)
    }
}
