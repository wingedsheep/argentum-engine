package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object HillGiant {
    val definition = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype.GIANT),
        power = 3,
        toughness = 3,
        metadata = ScryfallMetadata(
            collectorNumber = "133",
            rarity = Rarity.COMMON,
            artist = "Randy Gallegos",
            flavorText = "Hill giants are mostly just big. Of course, that does count for a lot!",
            imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd36579-c108-40c0-bce4-38ab837a8c65.jpg",
            scryfallId = "7cd36579-c108-40c0-bce4-38ab837a8c65",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Hill Giant") { }
}
