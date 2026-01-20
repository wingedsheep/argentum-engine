package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBlock
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object JungleLion {
    val definition = CardDefinition.creature(
        name = "Jungle Lion",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.CAT),
        power = 2,
        toughness = 1,
        oracleText = "Jungle Lion can't block.",
        metadata = ScryfallMetadata(
            collectorNumber = "171",
            rarity = Rarity.COMMON,
            artist = "Janine Johnston",
            flavorText = "The lion's only loyalty is to its hunger.",
            imageUri = "https://cards.scryfall.io/normal/front/6/1/613ceee3-92c7-46f1-8267-d6229ab15df5.jpg",
            scryfallId = "613ceee3-92c7-46f1-8267-d6229ab15df5",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Jungle Lion") {
        staticAbility(CantBlock(StaticTarget.SourceCreature))
    }
}
