package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageToAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Pyroclasm {
    val definition = CardDefinition.sorcery(
        name = "Pyroclasm",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Pyroclasm deals 2 damage to each creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "143",
            rarity = Rarity.RARE,
            artist = "John Matson",
            flavorText = "Chaos does not choose its enemies.",
            imageUri = "https://cards.scryfall.io/normal/front/d/e/de214247-e5e3-4d8f-935a-797218416be1.jpg",
            scryfallId = "de214247-e5e3-4d8f-935a-797218416be1",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Pyroclasm") {
        spell(DealDamageToAllCreaturesEffect(2))
    }
}
