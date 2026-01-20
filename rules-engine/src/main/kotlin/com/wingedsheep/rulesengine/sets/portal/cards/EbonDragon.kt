package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object EbonDragon {
    val definition = CardDefinition.creature(
        name = "Ebon Dragon",
        manaCost = ManaCost.parse("{5}{B}{B}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 5,
        toughness = 4,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen Ebon Dragon enters the battlefield, you may have target opponent discard a card.",
        metadata = ScryfallMetadata(
            collectorNumber = "91",
            rarity = Rarity.RARE,
            artist = "Donato Giancola",
            imageUri = "https://cards.scryfall.io/normal/front/4/f/4f10cf69-d3dc-43a4-9595-0f7d245c5efa.jpg",
            scryfallId = "4f10cf69-d3dc-43a4-9595-0f7d245c5efa",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Ebon Dragon") {
        keywords(Keyword.FLYING)
        triggered(
            trigger = OnEnterBattlefield(),
            effect = DiscardCardsEffect(1, EffectTarget.Opponent)
        )
    }
}
