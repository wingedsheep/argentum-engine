package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBeBlockedByColor
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SacredKnight {
    val definition = CardDefinition.creature(
        name = "Sacred Knight",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
        power = 3,
        toughness = 2,
        oracleText = "Sacred Knight can't be blocked by black creatures."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "24",
            rarity = Rarity.COMMON,
            artist = "Donato Giancola",
            flavorText = "No flame, no horror can sway me from my cause.",
            imageUri = "https://cards.scryfall.io/normal/front/5/7/57b19990-c4a2-433d-a9ce-005216e9e4ac.jpg",
            scryfallId = "57b19990-c4a2-433d-a9ce-005216e9e4ac",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Sacred Knight") {
        staticAbility(CantBeBlockedByColor(Color.BLACK))
    }
}
