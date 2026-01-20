package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object FootSoldiers {
    val definition = CardDefinition.creature(
        name = "Foot Soldiers",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 2,
        toughness = 4,
        metadata = ScryfallMetadata(
            collectorNumber = "16",
            rarity = Rarity.COMMON,
            artist = "Kev Walker",
            flavorText = "Infantry deployment is the art of putting your troops in the wrong place at the right time.",
            imageUri = "https://cards.scryfall.io/normal/front/4/5/458ddb33-66c4-4753-b1eb-8937ab812a81.jpg",
            scryfallId = "458ddb33-66c4-4753-b1eb-8937ab812a81",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Foot Soldiers") { }
}
