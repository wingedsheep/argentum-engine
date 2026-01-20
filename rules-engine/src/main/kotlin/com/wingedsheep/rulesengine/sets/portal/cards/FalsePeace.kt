package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.SkipCombatPhasesEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object FalsePeace {
    val definition = CardDefinition.sorcery(
        name = "False Peace",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "Target player skips all combat phases of their next turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "14",
            rarity = Rarity.COMMON,
            artist = "Zina Saunders",
            flavorText = "Mutual consent is not required for war.",
            imageUri = "https://cards.scryfall.io/normal/front/d/4/d4234262-56c6-4bd1-b425-12db931829d5.jpg",
            scryfallId = "d4234262-56c6-4bd1-b425-12db931829d5",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("False Peace") {
        spell(SkipCombatPhasesEffect(EffectTarget.AnyPlayer))
    }
}
