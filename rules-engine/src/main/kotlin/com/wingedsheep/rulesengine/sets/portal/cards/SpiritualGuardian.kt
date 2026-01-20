package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SpiritualGuardian {
    val definition = CardDefinition.creature(
        name = "Spiritual Guardian",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        subtypes = setOf(Subtype.SPIRIT),
        power = 3,
        toughness = 4,
        oracleText = "When Spiritual Guardian enters the battlefield, you gain 4 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "27",
            rarity = Rarity.RARE,
            artist = "Terese Nielsen",
            flavorText = "Hope is born within.",
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbea02f-9124-4e1a-8693-d988a0a3adae.jpg",
            scryfallId = "0dbea02f-9124-4e1a-8693-d988a0a3adae",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Spiritual Guardian") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = GainLifeEffect(4, EffectTarget.Controller)
        )
    }
}
