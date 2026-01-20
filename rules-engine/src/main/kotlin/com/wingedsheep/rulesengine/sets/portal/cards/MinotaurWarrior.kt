package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object MinotaurWarrior {
    val definition = CardDefinition.creature(
        name = "Minotaur Warrior",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.MINOTAUR, Subtype.WARRIOR),
        power = 2,
        toughness = 3,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "140",
            rarity = Rarity.COMMON,
            artist = "Scott M. Fischer",
            flavorText = "The herd's patience, the stampede's fury.",
            imageUri = "https://cards.scryfall.io/normal/front/c/6/c694f5db-a4ad-4abd-acff-d6b340d2387c.jpg",
            scryfallId = "c694f5db-a4ad-4abd-acff-d6b340d2387c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Minotaur Warrior") {
        // Vanilla creature - no abilities
    }
}
