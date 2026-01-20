package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CreatureGroupFilter
import com.wingedsheep.rulesengine.ability.GrantKeywordToGroupEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost

object NaturesCloak {
    val definition = CardDefinition.sorcery(
        name = "Nature's Cloak",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Green creatures you control gain forestwalk until end of turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "177",
            rarity = Rarity.RARE,
            artist = "Rebecca Guay",
            imageUri = "https://cards.scryfall.io/normal/front/1/d/1dfaba58-d0ab-4d1d-91dd-48543c862165.jpg",
            scryfallId = "1dfaba58-d0ab-4d1d-91dd-48543c862165",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Nature's Cloak") {
        spell(
            GrantKeywordToGroupEffect(
                keyword = Keyword.FORESTWALK,
                filter = CreatureGroupFilter.ColorYouControl(Color.GREEN),
                untilEndOfTurn = true
            )
        )
    }
}
