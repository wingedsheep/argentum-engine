package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object GoblinBully {
    val definition = CardDefinition.creature(
        name = "Goblin Bully",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype.GOBLIN),
        power = 2,
        toughness = 1,
        metadata = ScryfallMetadata(
            collectorNumber = "131",
            rarity = Rarity.COMMON,
            artist = "Pete Venters",
            flavorText = "It's easy to stand head and shoulders over those with no backbone.",
            imageUri = "https://cards.scryfall.io/normal/front/2/0/2094f2f6-8b38-47d6-973d-271986b5d982.jpg",
            scryfallId = "2094f2f6-8b38-47d6-973d-271986b5d982",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Goblin Bully") { }
}
