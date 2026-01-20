package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CanOnlyBlockCreaturesWithKeyword
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object CloudSpirit {
    val definition = CardDefinition.creature(
        name = "Cloud Spirit",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.SPIRIT),
        power = 3,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nCloud Spirit can block only creatures with flying."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "47",
            rarity = Rarity.UNCOMMON,
            artist = "DiTerlizzi",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc7547aa-fcf7-4b6e-955d-cc5ebc40cd7d.jpg",
            scryfallId = "cc7547aa-fcf7-4b6e-955d-cc5ebc40cd7d",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Cloud Spirit") {
        keywords(Keyword.FLYING)
        staticAbility(CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING))
    }
}
