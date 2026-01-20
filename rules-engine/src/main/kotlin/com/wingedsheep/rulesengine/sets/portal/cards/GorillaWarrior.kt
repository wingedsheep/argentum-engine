package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object GorillaWarrior {
    val definition = CardDefinition.creature(
        name = "Gorilla Warrior",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.of("Ape"), Subtype.WARRIOR),
        power = 3,
        toughness = 2,
        metadata = ScryfallMetadata(
            collectorNumber = "168",
            rarity = Rarity.COMMON,
            artist = "John Matson",
            flavorText = "They were formidable even before they learned the use of weapons.",
            imageUri = "https://cards.scryfall.io/normal/front/3/8/38f9c3f3-0d4d-4eec-bd14-9be3233178dc.jpg",
            scryfallId = "38f9c3f3-0d4d-4eec-bd14-9be3233178dc",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Gorilla Warrior") { }
}
