package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object StarlitAngel {
    val definition = CardDefinition.creature(
        name = "Starlit Angel",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        subtypes = setOf(Subtype.ANGEL),
        power = 3,
        toughness = 4,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "30",
            rarity = Rarity.UNCOMMON,
            artist = "Rebecca Guay",
            flavorText = "To soar as high as hope, to dive as swift as justice.",
            imageUri = "https://cards.scryfall.io/normal/front/3/6/36691cd0-c709-4452-a61a-d6e2049fdfcf.jpg",
            scryfallId = "36691cd0-c709-4452-a61a-d6e2049fdfcf",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Starlit Angel") {
        keywords(Keyword.FLYING)
    }
}
