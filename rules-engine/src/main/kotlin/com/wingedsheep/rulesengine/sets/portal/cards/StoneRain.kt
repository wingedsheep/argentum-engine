package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object StoneRain {
    val definition = CardDefinition.sorcery(
        name = "Stone Rain",
        manaCost = ManaCost.parse("{2}{R}"),
        oracleText = "Destroy target land."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "151",
            rarity = Rarity.COMMON,
            artist = "John Matson",
            flavorText = "I cast a thousand tiny suns—Beware my many dawns.",
            imageUri = "https://cards.scryfall.io/normal/front/5/7/57f84a13-d7dc-491b-a77c-1b99b6797d7e.jpg",
            scryfallId = "57f84a13-d7dc-491b-a77c-1b99b6797d7e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Stone Rain") {
        spell(DestroyEffect(EffectTarget.TargetLand))
    }
}
