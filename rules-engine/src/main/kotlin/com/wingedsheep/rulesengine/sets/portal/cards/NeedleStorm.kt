package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageToAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object NeedleStorm {
    val definition = CardDefinition.sorcery(
        name = "Needle Storm",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Needle Storm deals 4 damage to each creature with flying."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "179",
            rarity = Rarity.UNCOMMON,
            artist = "Charles Gillespie",
            imageUri = "https://cards.scryfall.io/normal/front/2/9/29a44e44-94b1-4bd2-8e00-6bd2ec07ee4c.jpg",
            scryfallId = "29a44e44-94b1-4bd2-8e00-6bd2ec07ee4c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Needle Storm") {
        spell(DealDamageToAllCreaturesEffect(4, onlyFlying = true))
    }
}
