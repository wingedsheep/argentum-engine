package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SkeletalCrocodile {
    val definition = CardDefinition.creature(
        name = "Skeletal Crocodile",
        manaCost = ManaCost.parse("{3}{B}"),
        subtypes = setOf(Subtype.CROCODILE, Subtype.of("Skeleton")),
        power = 5,
        toughness = 1,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "110",
            rarity = Rarity.COMMON,
            artist = "Mike Dringenberg",
            flavorText = "The less flesh there is, the more teeth there seem to be.",
            imageUri = "https://cards.scryfall.io/normal/front/e/b/ebcbbd6f-2915-4b4c-85d3-1d9b55d36c11.jpg",
            scryfallId = "ebcbbd6f-2915-4b4c-85d3-1d9b55d36c11",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Skeletal Crocodile") {
        // Vanilla creature - no abilities
    }
}
