package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object PantherWarriors {
    val definition = CardDefinition.creature(
        name = "Panther Warriors",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(Subtype.CAT, Subtype.WARRIOR),
        power = 6,
        toughness = 3,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "180",
            rarity = Rarity.COMMON,
            artist = "Eric Peterson",
            flavorText = "The dogs of war are nothing compared to the cats.",
            imageUri = "https://cards.scryfall.io/normal/front/8/b/8be610ce-5b84-416e-b427-98887642ff01.jpg",
            scryfallId = "8be610ce-5b84-416e-b427-98887642ff01",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Panther Warriors") {
        // Vanilla creature - no abilities
    }
}
