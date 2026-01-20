package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object PhantomWarrior {
    val definition = CardDefinition.creature(
        name = "Phantom Warrior",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype.of("Illusion"), Subtype.WARRIOR),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.CANT_BE_BLOCKED),
        oracleText = "Phantom Warrior can't be blocked."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "65",
            rarity = Rarity.RARE,
            artist = "Dan Frazier",
            flavorText = "The phantom warrior comes from nowhere and returns there just as quickly.",
            imageUri = "https://cards.scryfall.io/normal/front/6/d/6dbcb0df-d1cc-4718-ba1e-b590852cce20.jpg",
            scryfallId = "6dbcb0df-d1cc-4718-ba1e-b590852cce20",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Phantom Warrior") {
        keywords(Keyword.CANT_BE_BLOCKED)
    }
}
