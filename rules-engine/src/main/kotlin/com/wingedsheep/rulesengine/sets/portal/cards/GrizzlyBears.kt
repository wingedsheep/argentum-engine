package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object GrizzlyBears {
    val definition = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(
            collectorNumber = "169",
            rarity = Rarity.COMMON,
            artist = "Zina Saunders",
            flavorText = "Don't worry about provoking grizzly bears; they come that way.",
            imageUri = "https://cards.scryfall.io/normal/front/4/8/48e1b99c-97d0-48f2-bfdf-faa65bc0b608.jpg",
            scryfallId = "48e1b99c-97d0-48f2-bfdf-faa65bc0b608",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Grizzly Bears") { }
}
