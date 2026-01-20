package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyAllLandsOfTypeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Flashfires {
    val definition = CardDefinition.sorcery(
        name = "Flashfires",
        manaCost = ManaCost.parse("{3}{R}"),
        oracleText = "Destroy all Plains."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "129",
            rarity = Rarity.UNCOMMON,
            artist = "Randy Gallegos",
            flavorText = "Dry grass is tinder before the spark.",
            imageUri = "https://cards.scryfall.io/normal/front/a/9/a9e88867-6acb-43f8-806b-21480aaa1afc.jpg",
            scryfallId = "a9e88867-6acb-43f8-806b-21480aaa1afc",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Flashfires") {
        spell(DestroyAllLandsOfTypeEffect("Plains"))
    }
}
