package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object BogImp {
    val definition = CardDefinition.creature(
        name = "Bog Imp",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.IMP),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "81",
            rarity = Rarity.COMMON,
            artist = "Christopher Rush",
            flavorText = "Don't be fooled by their looks. Think of them as little knives with wings.",
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8681b3fd-33e5-4a45-8650-a4a142405096.jpg",
            scryfallId = "8681b3fd-33e5-4a45-8650-a4a142405096",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Bog Imp") {
        keywords(Keyword.FLYING)
    }
}
