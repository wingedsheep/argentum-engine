package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ArmoredPegasus {
    val definition = CardDefinition.creature(
        name = "Armored Pegasus",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.of("Pegasus")),
        power = 1,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying",
        metadata = ScryfallMetadata(
            collectorNumber = "6",
            rarity = Rarity.COMMON,
            artist = "Andrew Robinson",
            flavorText = "Asked how it survived a run-in with a bog imp, the pegasus just shook its mane and burped.",
            imageUri = "https://cards.scryfall.io/normal/front/a/8/a81b61af-cdb7-468f-9ff0-db82aa084023.jpg",
            scryfallId = "a81b61af-cdb7-468f-9ff0-db82aa084023",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Armored Pegasus") {
        keywords(Keyword.FLYING)
    }
}
