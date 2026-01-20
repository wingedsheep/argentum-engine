package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object LizardWarrior {
    val definition = CardDefinition.creature(
        name = "Lizard Warrior",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype.LIZARD, Subtype.WARRIOR),
        power = 4,
        toughness = 2,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "139",
            rarity = Rarity.COMMON,
            artist = "Roger Raupp",
            flavorText = "Don't let its appearance frighten you. Let its claws and teeth do that.",
            imageUri = "https://cards.scryfall.io/normal/front/0/5/053cd970-5b79-410b-8420-82d9a490b897.jpg",
            scryfallId = "053cd970-5b79-410b-8420-82d9a490b897",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Lizard Warrior") {
        // Vanilla creature - no abilities
    }
}
