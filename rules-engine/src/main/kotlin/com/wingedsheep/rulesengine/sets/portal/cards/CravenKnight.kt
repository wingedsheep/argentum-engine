package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBlock
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object CravenKnight {
    val definition = CardDefinition.creature(
        name = "Craven Knight",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
        power = 2,
        toughness = 2,
        oracleText = "Craven Knight can't block."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "85",
            rarity = Rarity.COMMON,
            artist = "Charles Gillespie",
            flavorText = "I say victory is better than honor.",
            imageUri = "https://cards.scryfall.io/normal/front/d/4/d4cbae27-4a1a-4e16-8876-9a2925c45302.jpg",
            scryfallId = "d4cbae27-4a1a-4e16-8876-9a2925c45302",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Craven Knight") {
        staticAbility(CantBlock())
    }
}
