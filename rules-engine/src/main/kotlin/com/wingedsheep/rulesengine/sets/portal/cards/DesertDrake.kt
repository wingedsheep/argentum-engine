package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object DesertDrake {
    val definition = CardDefinition.creature(
        name = "Desert Drake",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype.DRAKE),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "122",
            rarity = Rarity.UNCOMMON,
            artist = "Gerry Grace",
            flavorText = "Never doubt a dragon just because of its size.",
            imageUri = "https://cards.scryfall.io/normal/front/2/4/24673b35-aed2-40c0-a4ae-93bc4d392562.jpg",
            scryfallId = "24673b35-aed2-40c0-a4ae-93bc4d392562",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Desert Drake") {
        keywords(Keyword.FLYING)
    }
}
