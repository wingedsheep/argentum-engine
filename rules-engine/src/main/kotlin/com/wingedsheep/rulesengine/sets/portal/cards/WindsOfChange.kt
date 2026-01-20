package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.WheelEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object WindsOfChange {
    val definition = CardDefinition.sorcery(
        name = "Winds of Change",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Each player shuffles the cards from their hand into their library, then draws that many cards."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "156",
            rarity = Rarity.RARE,
            artist = "Adam Rex",
            imageUri = "https://cards.scryfall.io/normal/front/7/3/735b8aec-62d4-46db-9a68-a6c69cb6fd98.jpg",
            scryfallId = "735b8aec-62d4-46db-9a68-a6c69cb6fd98",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Winds of Change") {
        spell(WheelEffect())
    }
}
