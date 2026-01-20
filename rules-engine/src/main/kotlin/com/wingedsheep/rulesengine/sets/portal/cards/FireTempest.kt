package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageToAllEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object FireTempest {
    val definition = CardDefinition.sorcery(
        name = "Fire Tempest",
        manaCost = ManaCost.parse("{5}{R}{R}"),
        oracleText = "Fire Tempest deals 6 damage to each creature and each player."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "128",
            rarity = Rarity.RARE,
            artist = "Mike Dringenberg",
            imageUri = "https://cards.scryfall.io/normal/front/9/2/92334ebe-3d7a-46de-8b91-931e5d56a5a5.jpg",
            scryfallId = "92334ebe-3d7a-46de-8b91-931e5d56a5a5",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Fire Tempest") {
        spell(DealDamageToAllEffect(6))
    }
}
