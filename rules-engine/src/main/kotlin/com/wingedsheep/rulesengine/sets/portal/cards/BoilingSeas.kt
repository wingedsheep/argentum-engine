package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyAllLandsOfTypeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object BoilingSeas {
    val definition = CardDefinition.sorcery(
        name = "Boiling Seas",
        manaCost = ManaCost.parse("{3}{R}"),
        oracleText = "Destroy all Islands."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "119",
            rarity = Rarity.UNCOMMON,
            artist = "Tom Wänerstrand",
            flavorText = "What burns the land, boils the seas.",
            imageUri = "https://cards.scryfall.io/normal/front/d/1/d1523c1b-2ba1-4581-8502-47544d450d8e.jpg",
            scryfallId = "d1523c1b-2ba1-4581-8502-47544d450d8e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Boiling Seas") {
        spell(DestroyAllLandsOfTypeEffect("Island"))
    }
}
