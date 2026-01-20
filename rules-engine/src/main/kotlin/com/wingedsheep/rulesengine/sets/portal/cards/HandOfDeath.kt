package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object HandOfDeath {
    val definition = CardDefinition.sorcery(
        name = "Hand of Death",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Destroy target nonblack creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "96",
            rarity = Rarity.COMMON,
            artist = "John Coulthart",
            flavorText = "Death claims all but the darkest souls.",
            imageUri = "https://cards.scryfall.io/normal/front/2/7/27f136b8-52be-49b9-919b-2b9785254350.jpg",
            scryfallId = "27f136b8-52be-49b9-919b-2b9785254350",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Hand of Death") {
        spell(DestroyEffect(EffectTarget.TargetNonblackCreature))
    }
}
