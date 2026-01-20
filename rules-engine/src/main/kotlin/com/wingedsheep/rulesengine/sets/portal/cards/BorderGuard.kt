package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object BorderGuard {
    val definition = CardDefinition.creature(
        name = "Border Guard",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 1,
        toughness = 4,
        metadata = ScryfallMetadata(
            collectorNumber = "9",
            rarity = Rarity.COMMON,
            artist = "Kev Walker",
            flavorText = "\"Join the army, see foreign countries!\" they'd said.",
            imageUri = "https://cards.scryfall.io/normal/front/9/8/985af775-2036-459d-83c6-31ac84a0ffb1.jpg",
            scryfallId = "985af775-2036-459d-83c6-31ac84a0ffb1",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Border Guard") {
        // Vanilla creature - no abilities
    }
}
