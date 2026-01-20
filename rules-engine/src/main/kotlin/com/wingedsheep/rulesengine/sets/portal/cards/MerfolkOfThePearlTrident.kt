package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object MerfolkOfThePearlTrident {
    val definition = CardDefinition.creature(
        name = "Merfolk of the Pearl Trident",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype.MERFOLK),
        power = 1,
        toughness = 1,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "60",
            rarity = Rarity.COMMON,
            artist = "DiTerlizzi",
            flavorText = "Are merfolk humans with fins, or are humans merfolk with feet?",
            imageUri = "https://cards.scryfall.io/normal/front/1/2/126fec7a-4f36-49e5-a2d7-96deb7af856f.jpg",
            scryfallId = "126fec7a-4f36-49e5-a2d7-96deb7af856f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Merfolk of the Pearl Trident") {
        // Vanilla creature - no abilities
    }
}
