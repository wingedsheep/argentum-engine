package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SkeletalSnake {
    val definition = CardDefinition.creature(
        name = "Skeletal Snake",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.of("Snake"), Subtype.of("Skeleton")),
        power = 2,
        toughness = 1,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "111",
            rarity = Rarity.COMMON,
            artist = "John Matson",
            flavorText = "They watched as the snake shed layer after layer until only cold bone remained.",
            imageUri = "https://cards.scryfall.io/normal/front/4/2/42bd4896-4191-4479-be57-070753f8725c.jpg",
            scryfallId = "42bd4896-4191-4479-be57-070753f8725c",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Skeletal Snake") {
        // Vanilla creature - no abilities
    }
}
