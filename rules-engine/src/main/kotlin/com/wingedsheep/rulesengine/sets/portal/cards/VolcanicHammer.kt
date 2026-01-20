package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object VolcanicHammer {
    val definition = CardDefinition.sorcery(
        name = "Volcanic Hammer",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Volcanic Hammer deals 3 damage to any target."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "154",
            rarity = Rarity.COMMON,
            artist = "Christopher Rush",
            flavorText = "Cast the weight as though it were a die, to see a rival's fate.",
            imageUri = "https://cards.scryfall.io/normal/front/9/5/9563d7c1-4ed1-4919-b0b8-ea1ec9d4bbf6.jpg",
            scryfallId = "9563d7c1-4ed1-4919-b0b8-ea1ec9d4bbf6",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Volcanic Hammer") {
        spell(DealDamageEffect(3, EffectTarget.AnyTarget))
    }
}
