package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealXDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Blaze {
    val definition = CardDefinition.sorcery(
        name = "Blaze",
        manaCost = ManaCost.parse("{X}{R}"),
        oracleText = "Blaze deals X damage to any target."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "118",
            rarity = Rarity.UNCOMMON,
            artist = "Gerry Grace",
            flavorText = "Fire never dies alone.",
            imageUri = "https://cards.scryfall.io/normal/front/f/1/f175c959-3b5d-46a3-9194-fad2359bbff9.jpg",
            scryfallId = "f175c959-3b5d-46a3-9194-fad2359bbff9",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Blaze") {
        spell(DealXDamageEffect(EffectTarget.AnyTarget))
    }
}
