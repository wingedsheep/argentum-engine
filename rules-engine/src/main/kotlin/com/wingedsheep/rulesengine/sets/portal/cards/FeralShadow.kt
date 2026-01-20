package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object FeralShadow {
    val definition = CardDefinition.creature(
        name = "Feral Shadow",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype.of("Nightstalker")),
        power = 2,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "93",
            rarity = Rarity.COMMON,
            artist = "Colin MacNeil",
            flavorText = "Not all shadows are cast by light—some are cast by darkness.",
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f4c00-6bf8-440b-9761-b17a0e36c27e.jpg",
            scryfallId = "c46f4c00-6bf8-440b-9761-b17a0e36c27e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Feral Shadow") {
        keywords(Keyword.FLYING)
    }
}
