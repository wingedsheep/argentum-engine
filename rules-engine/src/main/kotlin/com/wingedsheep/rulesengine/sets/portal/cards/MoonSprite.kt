package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object MoonSprite {
    val definition = CardDefinition.creature(
        name = "Moon Sprite",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.FAERIE),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "174",
            rarity = Rarity.UNCOMMON,
            artist = "Terese Nielsen",
            flavorText = "I am that merry wanderer of the night. —William Shakespeare, A Midsummer Night's Dream",
            imageUri = "https://cards.scryfall.io/normal/front/f/0/f0944759-ee9f-4ae0-9d1b-2533ff6791a2.jpg",
            scryfallId = "f0944759-ee9f-4ae0-9d1b-2533ff6791a2",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Moon Sprite") {
        keywords(Keyword.FLYING)
    }
}
