package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object TouchOfBrilliance {
    val definition = CardDefinition.sorcery(
        name = "Touch of Brilliance",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "76",
            rarity = Rarity.COMMON,
            artist = "John Coulthart",
            flavorText = "Acting on one good idea is better than hoarding all the world's knowledge.",
            imageUri = "https://cards.scryfall.io/normal/front/1/9/196474ce-e28e-48f0-b407-dc5535adf1b6.jpg",
            scryfallId = "196474ce-e28e-48f0-b407-dc5535adf1b6",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Touch of Brilliance") {
        spell(DrawCardsEffect(2))
    }
}
