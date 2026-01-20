package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EachPlayerDrawsXEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Prosperity {
    val definition = CardDefinition.sorcery(
        name = "Prosperity",
        manaCost = ManaCost.parse("{X}{U}"),
        oracleText = "Each player draws X cards."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "66",
            rarity = Rarity.RARE,
            artist = "Phil Foglio",
            flavorText = "Life can never be too good.",
            imageUri = "https://cards.scryfall.io/normal/front/2/6/269bb4fc-9d8f-42cc-8f71-6a658e41533c.jpg",
            scryfallId = "269bb4fc-9d8f-42cc-8f71-6a658e41533c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Prosperity") {
        spell(EachPlayerDrawsXEffect())
    }
}
