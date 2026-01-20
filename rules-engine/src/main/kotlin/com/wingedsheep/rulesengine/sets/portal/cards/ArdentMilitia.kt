package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ArdentMilitia {
    val definition = CardDefinition.creature(
        name = "Ardent Militia",
        manaCost = ManaCost.parse("{4}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 2,
        toughness = 5,
        keywords = setOf(Keyword.VIGILANCE),
        oracleText = "Vigilance",
        metadata = ScryfallMetadata(
            collectorNumber = "4",
            rarity = Rarity.UNCOMMON,
            artist = "Mike Raabe",
            flavorText = "Some fight for honor and some for gold, but the militia fights for hearth and home.",
            imageUri = "https://cards.scryfall.io/normal/front/5/4/543f8c6a-bcf1-4400-82e5-83d36cb60464.jpg",
            scryfallId = "543f8c6a-bcf1-4400-82e5-83d36cb60464",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Ardent Militia") {
        keywords(Keyword.VIGILANCE)
    }
}
