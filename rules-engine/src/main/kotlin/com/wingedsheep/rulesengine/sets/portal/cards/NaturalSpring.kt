package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object NaturalSpring {
    val definition = CardDefinition.sorcery(
        name = "Natural Spring",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        oracleText = "Target player gains 8 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "176",
            rarity = Rarity.UNCOMMON,
            artist = "Janine Johnston",
            flavorText = "Though many have come to this place to wash their wounds, the water has never turned red.",
            imageUri = "https://cards.scryfall.io/normal/front/8/d/8ddfc1cc-5c13-443c-a0ae-0bcc931923e7.jpg",
            scryfallId = "8ddfc1cc-5c13-443c-a0ae-0bcc931923e7",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Natural Spring") {
        spell(GainLifeEffect(8, EffectTarget.AnyPlayer))
    }
}
