package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RegalUnicorn {
    val definition = CardDefinition.creature(
        name = "Regal Unicorn",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.UNICORN),
        power = 2,
        toughness = 3,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "22",
            rarity = Rarity.COMMON,
            artist = "Zina Saunders",
            flavorText = "Unicorns don't care if you believe in them any more than you care if they believe in you.",
            imageUri = "https://cards.scryfall.io/normal/front/d/a/daa1fb8c-12fa-4e9c-979f-55e89356acaf.jpg",
            scryfallId = "daa1fb8c-12fa-4e9c-979f-55e89356acaf",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Regal Unicorn") {
        // Vanilla creature - no abilities
    }
}
