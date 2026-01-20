package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object GiantOctopus {
    val definition = CardDefinition.creature(
        name = "Giant Octopus",
        manaCost = ManaCost.parse("{3}{U}"),
        subtypes = setOf(Subtype.OCTOPUS),
        power = 3,
        toughness = 3,
        metadata = ScryfallMetadata(
            collectorNumber = "56",
            rarity = Rarity.COMMON,
            artist = "John Matson",
            flavorText = "At the sight of the thing the calamari vendor's eyes went wide, but from fear or avarice none could tell.",
            imageUri = "https://cards.scryfall.io/normal/front/4/5/4528edca-cc36-4f63-9615-24ca315d672c.jpg",
            scryfallId = "4528edca-cc36-4f63-9615-24ca315d672c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Giant Octopus") { }
}
