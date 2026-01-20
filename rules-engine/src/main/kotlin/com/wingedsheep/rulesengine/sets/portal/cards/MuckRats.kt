package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object MuckRats {
    val definition = CardDefinition.creature(
        name = "Muck Rats",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype.RAT),
        power = 1,
        toughness = 1,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "102",
            rarity = Rarity.COMMON,
            artist = "Colin MacNeil",
            flavorText = "The difference between a nuisance and a threat is often merely a matter of numbers.",
            imageUri = "https://cards.scryfall.io/normal/front/d/4/d4041226-7ce2-46d1-8844-20fa50b6568a.jpg",
            scryfallId = "d4041226-7ce2-46d1-8844-20fa50b6568a",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Muck Rats") {
        // Vanilla creature - no abilities
    }
}
