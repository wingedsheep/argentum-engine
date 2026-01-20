package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object KnightErrant {
    val definition = CardDefinition.creature(
        name = "Knight Errant",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
        power = 2,
        toughness = 2,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "20",
            rarity = Rarity.COMMON,
            artist = "Dan Frazier",
            flavorText = ". . . [B]efore honor is humility. —The Bible, Proverbs 15:33",
            imageUri = "https://cards.scryfall.io/normal/front/9/c/9c31b4b4-18fc-4a6e-8d74-fd5340964320.jpg",
            scryfallId = "9c31b4b4-18fc-4a6e-8d74-fd5340964320",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Knight Errant") {
        // Vanilla creature - no abilities
    }
}
