package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object OwlFamiliar {
    val definition = CardDefinition.creature(
        name = "Owl Familiar",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.BIRD),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen Owl Familiar enters the battlefield, draw a card, then discard a card.",
        metadata = ScryfallMetadata(
            collectorNumber = "63",
            rarity = Rarity.COMMON,
            artist = "Janine Johnston",
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9587bcb-0ece-4b36-85dc-76899e403b08.jpg",
            scryfallId = "d9587bcb-0ece-4b36-85dc-76899e403b08",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Owl Familiar") {
        keywords(Keyword.FLYING)
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CompositeEffect(listOf(
                DrawCardsEffect(1, EffectTarget.Controller),
                DiscardCardsEffect(1, EffectTarget.Controller)
            ))
        )
    }
}
