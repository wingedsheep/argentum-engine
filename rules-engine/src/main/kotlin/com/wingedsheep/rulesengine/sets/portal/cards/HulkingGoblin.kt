package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBlock
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object HulkingGoblin {
    val definition = CardDefinition.creature(
        name = "Hulking Goblin",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype.GOBLIN),
        power = 2,
        toughness = 2,
        oracleText = "Hulking Goblin can't block."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "135",
            rarity = Rarity.COMMON,
            artist = "Pete Venters",
            flavorText = "The bigger they are, the harder they avoid work.",
            imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3eead8-7e07-4463-9e67-c396d2d7931e.jpg",
            scryfallId = "8e3eead8-7e07-4463-9e67-c396d2d7931e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Hulking Goblin") {
        staticAbility(CantBlock())
    }
}
