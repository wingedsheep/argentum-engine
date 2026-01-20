package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object DevotedHero {
    val definition = CardDefinition.creature(
        name = "Devoted Hero",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 1,
        toughness = 2,
        metadata = ScryfallMetadata(
            collectorNumber = "13",
            rarity = Rarity.COMMON,
            artist = "DiTerlizzi",
            flavorText = "The heart's courage is the soul's guardian.",
            imageUri = "https://cards.scryfall.io/normal/front/8/9/89fb4bf8-51b6-44a8-92ac-e5aec4e4f2bc.jpg",
            scryfallId = "89fb4bf8-51b6-44a8-92ac-e5aec4e4f2bc",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Devoted Hero") {
        // Vanilla creature - no abilities
    }
}
