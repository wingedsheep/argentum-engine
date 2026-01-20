package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object HighlandGiant {
    val definition = CardDefinition.creature(
        name = "Highland Giant",
        manaCost = ManaCost.parse("{2}{R}{R}"),
        subtypes = setOf(Subtype.GIANT),
        power = 3,
        toughness = 4,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "132",
            rarity = Rarity.COMMON,
            artist = "Ron Spencer",
            flavorText = "Though slow-witted and slow-moving, they are quick to anger.",
            imageUri = "https://cards.scryfall.io/normal/front/3/2/32f49716-1522-4f36-92c9-63ef2059c4ef.jpg",
            scryfallId = "32f49716-1522-4f36-92c9-63ef2059c4ef",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Highland Giant") {
        // Vanilla creature - no abilities
    }
}
