package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object EliteCatWarrior {
    val definition = CardDefinition.creature(
        name = "Elite Cat Warrior",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.CAT, Subtype.WARRIOR),
        power = 2,
        toughness = 3,
        keywords = setOf(Keyword.FORESTWALK),
        oracleText = "Forestwalk"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "163",
            rarity = Rarity.COMMON,
            artist = "Eric Peterson",
            flavorText = "Hear that? No? That's a cat warrior.",
            imageUri = "https://cards.scryfall.io/normal/front/7/3/7396c4e9-b0d8-4b8f-8c17-6f913a967b17.jpg",
            scryfallId = "7396c4e9-b0d8-4b8f-8c17-6f913a967b17",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Elite Cat Warrior") {
        keywords(Keyword.FORESTWALK)
    }
}
