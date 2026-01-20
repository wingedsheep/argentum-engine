package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object Anaconda {
    val definition = CardDefinition.creature(
        name = "Anaconda",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype.SERPENT),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.SWAMPWALK),
        oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)",
        metadata = ScryfallMetadata(
            collectorNumber = "158",
            rarity = Rarity.UNCOMMON,
            artist = "Andrew Robinson",
            flavorText = "Something soft bumped against the rowboat, then was gone.",
            imageUri = "https://cards.scryfall.io/normal/front/0/a/0a2012ad-6425-4935-83af-fc7309ec2ece.jpg",
            scryfallId = "0a2012ad-6425-4935-83af-fc7309ec2ece",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Anaconda") {
        keywords(Keyword.SWAMPWALK)
    }
}
