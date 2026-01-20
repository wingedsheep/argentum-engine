package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.LookAtTopCardsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object AncestralMemories {
    val definition = CardDefinition.sorcery(
        name = "Ancestral Memories",
        manaCost = ManaCost.parse("{2}{U}{U}{U}"),
        oracleText = "Look at the top seven cards of your library. Put two of them into your hand and the rest into your graveyard."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "40",
            rarity = Rarity.RARE,
            artist = "Dan Frazier",
            imageUri = "https://cards.scryfall.io/normal/front/c/f/cf9b613c-61bf-4c2d-9c90-2949e442aea5.jpg",
            scryfallId = "cf9b613c-61bf-4c2d-9c90-2949e442aea5",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Ancestral Memories") {
        spell(LookAtTopCardsEffect(7, 2))
    }
}
