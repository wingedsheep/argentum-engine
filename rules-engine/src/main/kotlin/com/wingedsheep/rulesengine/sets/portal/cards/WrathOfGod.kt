package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object WrathOfGod {
    val definition = CardDefinition.sorcery(
        name = "Wrath of God",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        oracleText = "Destroy all creatures. They can't be regenerated."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "39",
            rarity = Rarity.RARE,
            artist = "Mike Raabe",
            flavorText = "\"As flies to wanton boys, are we to the gods. They kill us for their sport.\" — William Shakespeare, King Lear",
            imageUri = "https://cards.scryfall.io/normal/front/d/7/d75d8204-6f9d-4a7a-bb8b-d51ac65a30fa.jpg",
            scryfallId = "d75d8204-6f9d-4a7a-bb8b-d51ac65a30fa",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Wrath of God") {
        spell(DestroyAllCreaturesEffect)
    }
}
