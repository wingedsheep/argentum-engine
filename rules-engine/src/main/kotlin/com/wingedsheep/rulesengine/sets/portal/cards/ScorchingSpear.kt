package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object ScorchingSpear {
    val definition = CardDefinition.sorcery(
        name = "Scorching Spear",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Scorching Spear deals 1 damage to any target."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "148",
            rarity = Rarity.COMMON,
            artist = "Mike Raabe",
            flavorText = "Lift your spear as you might lift your glass, and toast your enemy.",
            imageUri = "https://cards.scryfall.io/normal/front/9/e/9e4817bd-68e8-4a85-983a-ee6dda2bbf33.jpg",
            scryfallId = "9e4817bd-68e8-4a85-983a-ee6dda2bbf33",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Scorching Spear") {
        spell(DealDamageEffect(1, EffectTarget.AnyTarget))
    }
}
