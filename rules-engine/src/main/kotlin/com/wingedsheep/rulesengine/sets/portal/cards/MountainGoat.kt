package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object MountainGoat {
    val definition = CardDefinition.creature(
        name = "Mountain Goat",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype.of("Goat")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.MOUNTAINWALK),
        oracleText = "Mountainwalk"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "141",
            rarity = Rarity.UNCOMMON,
            artist = "Una Fricker",
            flavorText = "Only the heroic and the mad follow mountain goat trails.",
            imageUri = "https://cards.scryfall.io/normal/front/3/2/325100f1-d424-4db0-bfa9-24877156c0ba.jpg",
            scryfallId = "325100f1-d424-4db0-bfa9-24877156c0ba",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Mountain Goat") {
        keywords(Keyword.MOUNTAINWALK)
    }
}
