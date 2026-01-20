package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.LoseHalfLifeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object CruelBargain {
    val definition = CardDefinition.sorcery(
        name = "Cruel Bargain",
        manaCost = ManaCost.parse("{B}{B}{B}"),
        oracleText = "Draw four cards. You lose half your life, rounded up."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "86",
            rarity = Rarity.RARE,
            artist = "Adrian Smith",
            imageUri = "https://cards.scryfall.io/normal/front/9/6/96837a9e-dd68-4ce8-b760-0e1c22837164.jpg",
            scryfallId = "96837a9e-dd68-4ce8-b760-0e1c22837164",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Cruel Bargain") {
        spell(
            DrawCardsEffect(4) then
            LoseHalfLifeEffect(roundUp = true)
        )
    }
}
