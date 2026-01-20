package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object RowanTreefolk {
    val definition = CardDefinition.creature(
        name = "Rowan Treefolk",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype.TREEFOLK),
        power = 3,
        toughness = 4,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "184",
            rarity = Rarity.COMMON,
            artist = "Gerry Grace",
            flavorText = "One of the forest's best protectors is the forest itself.",
            imageUri = "https://cards.scryfall.io/normal/front/8/5/852a0956-8558-4754-ab57-6f1cc4de740e.jpg",
            scryfallId = "852a0956-8558-4754-ab57-6f1cc4de740e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Rowan Treefolk") {
        // Vanilla creature - no abilities
    }
}
