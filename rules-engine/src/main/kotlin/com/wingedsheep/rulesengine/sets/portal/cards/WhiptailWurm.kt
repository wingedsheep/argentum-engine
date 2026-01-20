package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WhiptailWurm {
    val definition = CardDefinition.creature(
        name = "Whiptail Wurm",
        manaCost = ManaCost.parse("{6}{G}"),
        subtypes = setOf(Subtype.WURM),
        power = 8,
        toughness = 5,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "192",
            rarity = Rarity.UNCOMMON,
            artist = "Una Fricker",
            flavorText = "It's hard to say for certain which end is more dangerous.",
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e76072-e76d-485e-b94c-c29849bc8a6f.jpg",
            scryfallId = "a1e76072-e76d-485e-b94c-c29849bc8a6f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Whiptail Wurm") {
        // Vanilla creature - no abilities
    }
}
