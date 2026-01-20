package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ArrogantVampire {
    val definition = CardDefinition.creature(
        name = "Arrogant Vampire",
        manaCost = ManaCost.parse("{3}{B}{B}"),
        subtypes = setOf(Subtype.of("Vampire")),
        power = 4,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying",
        metadata = ScryfallMetadata(
            collectorNumber = "79",
            rarity = Rarity.UNCOMMON,
            artist = "Zina Saunders",
            flavorText = "Charm and grace may hide a foul heart.",
            imageUri = "https://cards.scryfall.io/normal/front/e/7/e7342875-d49b-4fa7-a2fb-8dfc5e3d6e4f.jpg",
            scryfallId = "e7342875-d49b-4fa7-a2fb-8dfc5e3d6e4f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Arrogant Vampire") {
        keywords(Keyword.FLYING)
    }
}
