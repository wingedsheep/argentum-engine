package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WallOfSwords {
    val definition = CardDefinition.creature(
        name = "Wall of Swords",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.WALL),
        power = 3,
        toughness = 5,
        keywords = setOf(Keyword.DEFENDER, Keyword.FLYING),
        oracleText = "Defender\nFlying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "37",
            rarity = Rarity.UNCOMMON,
            artist = "Douglas Shuler",
            flavorText = "Sharper than wind, lighter than air.",
            imageUri = "https://cards.scryfall.io/normal/front/3/e/3e8d55a3-0d7f-4fba-9879-9a8264110e78.jpg",
            scryfallId = "3e8d55a3-0d7f-4fba-9879-9a8264110e78",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Wall of Swords") {
        keywords(Keyword.DEFENDER, Keyword.FLYING)
    }
}
