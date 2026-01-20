package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object BeeSting {
    val definition = CardDefinition.sorcery(
        name = "Bee Sting",
        manaCost = ManaCost.parse("{3}{G}"),
        oracleText = "Bee Sting deals 2 damage to any target."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "159",
            rarity = Rarity.UNCOMMON,
            artist = "Phil Foglio",
            flavorText = "There are few things as motivating as a swarm of bees.",
            imageUri = "https://cards.scryfall.io/normal/front/2/3/23bcf64a-ae3d-4abb-acc7-81bba237f37b.jpg",
            scryfallId = "23bcf64a-ae3d-4abb-acc7-81bba237f37b",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Bee Sting") {
        spell(DealDamageEffect(2, EffectTarget.AnyTarget))
    }
}
