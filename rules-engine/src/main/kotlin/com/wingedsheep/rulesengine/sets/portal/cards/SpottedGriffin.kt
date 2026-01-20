package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SpottedGriffin {
    val definition = CardDefinition.creature(
        name = "Spotted Griffin",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.GRIFFIN),
        power = 2,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "28",
            rarity = Rarity.COMMON,
            artist = "William Simpson",
            flavorText = "When the cat flies and the bird stalks, guard your horses carefully.",
            imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5b708b-368f-48d2-8eca-40f2ae6d5178.jpg",
            scryfallId = "4f5b708b-368f-48d2-8eca-40f2ae6d5178",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Spotted Griffin") {
        keywords(Keyword.FLYING)
    }
}
